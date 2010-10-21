/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of stachord, an independent implementation of         *
 * the Chord protocol (http://pdos.csail.mit.edu/chord/).                  *
 *                                                                         *
 * stachord is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * stachord is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with stachord.  If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                         *
 ***************************************************************************/

package uk.ac.standrews.cs.stachord.impl;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Observable;

import uk.ac.standrews.cs.nds.events.Event;
import uk.ac.standrews.cs.nds.events.IEvent;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Implementation of Chord node.
 *
 * @author Stephanie Anderson
 * @author Stuart Norcross (stuart@cs.st-andrews.ac.uk)
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
class ChordNodeImpl extends Observable implements IChordNode, IChordRemote {

    private final InetSocketAddress local_address; // The address of this node.
    private final IKey key; // The key of this node.
    private final int hash_code; // The hash code of this node.
    private IChordRemoteReference self_reference; // A local RMI reference to this node.
    private IChordRemoteReference predecessor; // The predecessor of this node.
    private IChordRemoteReference successor; // The successor of this node.
    private final SuccessorList successor_list; // The successor list of this node.
    private final FingerTable finger_table; // The finger table of this node.
    private boolean predecessor_maintenance_enabled = true; // Whether periodic predecessor maintenance should be performed.
    private boolean stabilization_enabled = true; // Whether periodic ring stabilization should be performed.
    private boolean finger_table_maintenance_enabled = true; // Whether periodic finger table maintenance should be performed.
    private boolean detailed_to_string = false; // Whether toString() should return a detailed description.

    private static final String PREDECESSOR_CHANGE_EVENT_TYPE = "PREDECESSOR_CHANGE_EVENT";
    private static final String SUCCESSOR_CHANGE_EVENT_TYPE = "SUCCESSOR_CHANGE_EVENT";
    private static final String SUCCESSOR_LIST_CHANGE_EVENT_TYPE = "SUCCESSOR_LIST_CHANGE_EVENT";
    private static final String FINGER_TABLE_CHANGE_EVENT_TYPE = "FINGER_TABLE_CHANGE_EVENT";

    // -------------------------------------------------------------------------------------------------------

    /**
     * The name of the remotely accessible Chord service.
     */
    static final String CHORD_REMOTE_SERVICE_NAME = IChordRemote.class.getSimpleName();

    /**
     * Predecessor change event.
     */
    public static final IEvent PREDECESSOR_CHANGE_EVENT = new Event(PREDECESSOR_CHANGE_EVENT_TYPE);

    /**
     * Successor change event.
     */
    public static final IEvent SUCCESSOR_CHANGE_EVENT = new Event(SUCCESSOR_CHANGE_EVENT_TYPE);

    /**
     * Successor list change event.
     */
    public static final IEvent SUCCESSOR_LIST_CHANGE_EVENT = new Event(SUCCESSOR_LIST_CHANGE_EVENT_TYPE);

    /**
     * Finger table change event.
     */
    public static final IEvent FINGER_TABLE_CHANGE_EVENT = new Event(FINGER_TABLE_CHANGE_EVENT_TYPE);

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new Chord node bound to the given local address, with a key generated by hashing the address.
     *     * @param local_address the local address
     * @throws RemoteException if the new node cannot be exposed for remote access
     */
    public ChordNodeImpl(final InetSocketAddress local_address) throws RemoteException {

        this(local_address, new SHA1KeyFactory().generateKey(local_address));
    }

    /**
     * Creates a new Chord node bound to the given local address, with the given key.
     *     * @param local_address the local address
     * @param key the key
     * @throws RemoteException if the new node cannot be exposed for remote access
     */
    public ChordNodeImpl(final InetSocketAddress local_address, final IKey key) throws RemoteException {

        this.local_address = local_address;
        this.key = key;
        hash_code = hashCode();
        successor_list = new SuccessorList(this);
        finger_table = new FingerTable(this);
        try {
            self_reference = new ChordRemoteReference(key, this);
        }
        catch (final RemoteException e) {
            ErrorHandling.hardExceptionError(e, "Unexpected remote exception when creating self-reference");
        }
        createRing();
        exposeNode();
        addObserver(this);
        startMaintenanceThread();
    }

    // -------------------------------------------------------------------------------------------------------

    // IChordNode operations.
    @Override
    public IKey getKey() {

        return key;
    }

    @Override
    public IChordRemoteReference lookup(final IKey k) throws RemoteException {

        if (k.equals(key) || successor.getKey().equals(key)) {

            // If the key is equal to this node's, or the ring currently only has one node...
            return self_reference;
        }
        return findSuccessor(k);
    }

    @Override
    public IChordRemoteReference getSuccessor() {

        return successor;
    }

    @Override
    public IChordRemoteReference getPredecessor() {

        return predecessor;
    }

    @Override
    public synchronized void join(final IChordRemoteReference known_node) throws RemoteException {

        // Route to this node's key; the result is this node's new successor.
        final IChordRemoteReference new_successor = known_node.getRemote().lookup(key);

        // Check that the new successor is not this node. This could happen if this node is already in a ring containing the known node.
        // This could happen in a situation where we're trying to combine two rings by having in a node in one join using a node in the
        // other as the known node, but where they're actually in the same ring. Perhaps unlikely, but we can never be completely sure
        // whether a ring has partitioned or not.
        if (!equals(new_successor.getRemote())) {
            setSuccessor(new_successor);
        }
    }

    @Override
    public IChordRemoteReference getSelfReference() {

        return self_reference;
    }

    @Override
    public void shutDown() {

        // Stop the maintenance thread.
        shutdownMaintenanceThread();

        // Unhook the node from RMI.
        try {
            final Registry registry = LocateRegistry.getRegistry(local_address.getHostName(), local_address.getPort());
            registry.unbind(CHORD_REMOTE_SERVICE_NAME);
        }
        catch (final Exception e) {
            ErrorHandling.exceptionError(e, "failed to destroy node: ", key);
        }

        Diagnostic.trace(DiagnosticLevel.FULL, "shutdown node: ", key);
    }

    /**
     * Checks whether the given key lies in this node's key range.
     *     * @param k a key
     * @return true if the key lies in this node's key range
     */
    @Override
    public boolean inLocalKeyRange(final IKey k) {

        // This is never called when the predecessor is null.
        return RingArithmetic.inHalfOpenSegment(k, predecessor.getKey(), key);
    }

    // -------------------------------------------------------------------------------------------------------

    // IChordRemote operations.
    @Override
    public InetSocketAddress getAddress() {

        return local_address;
    }

    @Override
    public void notify(final IChordRemoteReference potential_predecessor) {

        /* Case: predecessor is null and potential_predecessor is this node.
           We have a one-node ring, and the predecessor should stay null.

           Case: predecessor is null and potential_predecessor is not this node.
           Ring has at least two nodes, so any predecessor is better than nothing.
                     Case: predecessor is not null and potential_predecessor is this node.
           Ignore, since a node's predecessor is never itself.
                     Case: predecessor is not null and potential_predecessor is not in this node's current key range.
           Ignore, since the current predecessor doesn't appear to have failed, so only valid case is a
           new node joining.
                     Case: predecessor is not null and potential_predecessor is in this node's current key range.
           A new node has joined between the current predecessor and this node.
         */
        final IKey key_of_potential_predecessor = potential_predecessor.getKey();

        if (!key_of_potential_predecessor.equals(key) && (predecessor == null || inLocalKeyRange(key_of_potential_predecessor))) {
            setPredecessor(potential_predecessor);
        }
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() {

        return successor_list.getList();
    }

    @Override
    public List<IChordRemoteReference> getFingerList() {

        return finger_table.getFingers();
    }

    @Override
    public void isAlive() {

    }

    @Override
    public NextHopResult nextHop(final IKey k) {

        // Check whether the key lies in the range between this node and its successor,
        // in which case the successor represents the final hop.
        if (inSuccessorKeyRange(k)) { return new NextHopResult(successor, true); }
        return new NextHopResult(closestPrecedingNode(k), false);
    }

    @Override
    public void enablePredecessorMaintenance(final boolean enabled) {

        predecessor_maintenance_enabled = enabled;
    }

    @Override
    public void enableStabilization(final boolean enabled) {

        stabilization_enabled = enabled;
    }

    @Override
    public void enablePeerStateMaintenance(final boolean enabled) {

        finger_table_maintenance_enabled = enabled;
    }

    @Override
    public void notifyFailure(final IChordRemoteReference node) {

        finger_table.fingerFailure(node);
    }

    @Override
    public String toStringDetailed() {

        final StringBuilder builder = new StringBuilder();
        builder.append("Node state\nkey: ");
        builder.append(key);
        builder.append("\nlocal_address: ");
        builder.append(local_address);
        builder.append("\npredecessor: ");
        builder.append(predecessor != null ? predecessor.getAddress() : "null");
        builder.append("\nsuccessor: ");
        builder.append(successor != null ? successor.getAddress() : "null");
        builder.append("\nsuccessor_list: ");
        builder.append(successor_list);
        builder.append("\nfinger_table: ");
        builder.append(finger_table);
        return builder.toString();
    }

    @Override
    public String toStringTerse() {

        return "key: " + key + " local_address: " + local_address;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public int hashCode() {

        return hash_code;
    }

    @Override
    public String toString() {

        return detailed_to_string ? toStringDetailed() : toStringTerse();
    }

    @Override
    public boolean equals(final Object other) {

        try {
            return other instanceof IChordRemote && ((IChordRemote) other).getKey().equals(key);
        }
        catch (final RemoteException e) {
            return false;
        }
    }

    public void showState() {

        System.out.println(toStringDetailed());
    }

    public void setToStringDetailed(final boolean detailed) {

        detailed_to_string = detailed;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public void update(final Observable o, final Object arg) {

        final String event_type = ((Event) arg).getType();
        Diagnostic.trace(DiagnosticLevel.FULL, ">>>>>>>>>>>>>>>>>>>>>> update: " + event_type);
        if (event_type.equals(SUCCESSOR_CHANGE_EVENT_TYPE)) {
            Diagnostic.trace(DiagnosticLevel.FULL, "successor now: ", (successor != null ? successor.getKey() : "null"));
        }
        if (event_type.equals(PREDECESSOR_CHANGE_EVENT_TYPE)) {
            Diagnostic.trace(DiagnosticLevel.FULL, "predecessor now: ", (predecessor != null ? predecessor.getKey() : "null"));
        }
        if (event_type.equals(SUCCESSOR_LIST_CHANGE_EVENT_TYPE)) {
            Diagnostic.trace(DiagnosticLevel.FULL, "successor list now: ", successor_list);
        }
        if (event_type.equals(FINGER_TABLE_CHANGE_EVENT_TYPE)) {
            Diagnostic.trace(DiagnosticLevel.FULL, "finger table now: ", finger_table);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Sets data structures for a new ring.
     */
    private synchronized void createRing() {

        setPredecessor(null);
        setSuccessor(self_reference);
        successor_list.clear();
        setChanged();
        notifyObservers(SUCCESSOR_LIST_CHANGE_EVENT);
    }

    /**
      * Exposes this node for remote RMI access.
      *      * @throws RemoteException if the node cannot be exposed for remote access
      */
    private void exposeNode() throws RemoteException {

        // Get RMI registry.
        final Registry local_registry = LocateRegistry.createRegistry(local_address.getPort());

        // Start RMI listening. NOTE the result of getRemote() is actually local!
        UnicastRemoteObject.exportObject(getSelfReference().getRemote(), 0);

        // Register the service with the registry.
        local_registry.rebind(CHORD_REMOTE_SERVICE_NAME, getSelfReference().getRemote());
    }

    /**
     * Executes the stabilization protocol.
     */
    private synchronized void stabilize() {

        try {
            // Find predecessor of this node's successor.
            final IChordRemoteReference predecessor_of_successor = getPredecessorOfSuccessor();

            // Check whether that is a better successor for this node than its current successor.
            // This may update this node's successor pointer.
            checkForBetterSuccessor(predecessor_of_successor);

            // Notify this node's successor (which may have just changed) that this node may be its predecessor.
            notifySuccessor();

            // Update this node's successor list from its successor's.
            refreshSuccessorList();
        }
        catch (final RemoteException e) {

            // Error contacting successor.
            handleSuccessorError(e);
        }
    }

    /**
     * Tries to communicate with this node's predecessor.
     */
    private synchronized void checkPredecessor() {

        try {
            pingPredecessor();
        }
        catch (final RemoteException e) {
            setPredecessor(null);
        }
    }

    /**
     * Checks the next finger, and updates the finger table entry if a better finger is available.
     */
    private synchronized void fixNextFinger() {

        if (finger_table.fixNextFinger()) {
            setChanged();
            notifyObservers(FINGER_TABLE_CHANGE_EVENT);
        }
    }

    /**
     * Sets a new predecessor for this node.
     *     * @param new_predecessor the new predecessor
     */
    private synchronized void setPredecessor(final IChordRemoteReference new_predecessor) {

        final IChordRemoteReference old_predecessor = predecessor;
        predecessor = new_predecessor;
        if (new_predecessor == null || !new_predecessor.equals(old_predecessor)) {
            setChanged();
            notifyObservers(PREDECESSOR_CHANGE_EVENT);
        }
    }

    /**
     * Returns the closest preceding node from the finger table, or the successor if there is no such node in the finger table.
     * @param k a key
     * @return the peer node whose key most closely precedes k
     */
    private IChordRemoteReference closestPrecedingNode(final IKey k) {

        try {
            return finger_table.closestPrecedingNode(k);
        }
        catch (final NoPrecedingNodeException e) {
            return successor;
        }
    }

    /**
     * Attempts to communicate with this node's predecessor.
     *     * @throws RemoteException if the attempted communication fails
     */
    private void pingPredecessor() throws RemoteException {

        if (predecessor != null) {
            predecessor.getRemote().isAlive();
        }
    }

    /**
     * Gets the predecessor of this node's successor.
     *     * @return the predecessor of this node's successor
     * @throws RemoteException if an error occurs communicating with the successor
     */
    private IChordRemoteReference getPredecessorOfSuccessor() throws RemoteException {

        return successor.getRemote().getPredecessor();
    }

    /**
     * Checks whether a given potential successor would be a better successor for this node than the current successor.
     *     * @param potential_successor the potential successor
     */
    private void checkForBetterSuccessor(final IChordRemoteReference potential_successor) {

        if (potential_successor != null) {

            final IKey key_of_potential_successor = potential_successor.getKey();

            // Check whether the potential successor's key lies in this node's current successor's key range, and the potential successor is
            // not the current successor.
            if (inSuccessorKeyRange(key_of_potential_successor) && !key_of_potential_successor.equals(successor.getKey())) {

                // The potential successor is more suitable as this node's successor.
                setSuccessor(potential_successor);
            }
        }
    }

    private void notifySuccessor() throws RemoteException {

        successor.getRemote().notify(self_reference);
    }

    private void refreshSuccessorList() {

        if (!successor.getKey().equals(getKey())) {
            try {
                final List<IChordRemoteReference> successor_list_of_successor = successor.getRemote().getSuccessorList();

                if (successor_list.refreshList(successor_list_of_successor)) {
                    setChanged();
                    notifyObservers(SUCCESSOR_LIST_CHANGE_EVENT);
                }
            }
            catch (final RemoteException e) {
                handleSuccessorError(e);
            }
        }
    }

    private void handleSuccessorError(final Exception e) {

        Diagnostic.trace(DiagnosticLevel.FULL, this, ": error calling successor ", successor, ": ", e);
        findWorkingSuccessor();
    }

    private IChordRemoteReference findSuccessor(final IKey key) throws RemoteException {

        // Get the first hop.
        NextHopResult next_hop = nextHop(key);

        // Keep track of the hop before the next one, in case the next one turns out to have failed
        // and the one before it has to be notified so it can update its finger table.
        IChordRemote current_hop = this;

        while (!next_hop.isFinalHop()) {
            try {

                // Remember the previous value of next_hop.
                final IChordRemote previous_next_hop = next_hop.getNode().getRemote();

                next_hop = previous_next_hop.nextHop(key);
                current_hop = previous_next_hop;
            }
            catch (final RemoteException e) {
                current_hop.notifyFailure(next_hop.getNode());
                throw e;
            }
        }
        return next_hop.getNode();
    }

    /**
     * Sets the successor node in key space.
     *     * @param successor the new successor node
     */
    private synchronized void setSuccessor(final IChordRemoteReference successor) {

        assert successor != null;
        final IChordRemoteReference oldSuccessor = this.successor;
        this.successor = successor;
        setChanged();
        if (oldSuccessor != null && !oldSuccessor.equals(successor)) {
            notifyObservers(SUCCESSOR_CHANGE_EVENT);
        }
    }

    private boolean inSuccessorKeyRange(final IKey k) {

        return RingArithmetic.inHalfOpenSegment(k, key, successor.getKey());
    }

    /**
     * Attempts to find a working successor from the successor list, or failing that using the predecessor or a finger.
     */
    private synchronized void findWorkingSuccessor() {

        try {
            final IChordRemoteReference new_successor = successor_list.findFirstWorkingNode();
            setSuccessor(new_successor);
        }
        catch (final NoReachableNodeException e) {
            try {
                join(predecessor);
            }
            catch (final Exception e1) {

                // RemoteException if predecessor has failed, or NullPointerException if it's already null
                try {
                    joinUsingFinger();
                }
                catch (final NoReachableNodeException e2) {

                    // Couldn't contact any known node in current ring, so admit defeat and partition ring.
                    createRing();
                }
            }
        }
    }

    private void joinUsingFinger() throws NoReachableNodeException {

        for (final IChordRemoteReference finger : finger_table.getFingers()) {
            if (finger != null && !finger.getKey().equals(getKey())) {
                try {
                    join(finger);
                    return;
                }
                catch (final RemoteException e) {
                    // Ignore and try next finger. Disable PMD warning - NOPMD
                }
            }
        }
        throw new NoReachableNodeException();
    }

    private void startMaintenanceThread() {

        new Thread() {

            public static final int WAIT_PERIOD = 1000;

            @Override
            public void run() {

                while (predecessorMaintenanceEnabled() || stabilizationEnabled() || fingerTableMaintenanceEnabled()) {
                    try {
                        sleep(WAIT_PERIOD);
                    }
                    catch (final InterruptedException e) {
                    }
                    if (predecessorMaintenanceEnabled()) {
                        checkPredecessor();
                    }
                    if (stabilizationEnabled()) {
                        stabilize();
                    }
                    if (fingerTableMaintenanceEnabled()) {
                        fixNextFinger();
                    }
                }
                Diagnostic.trace(DiagnosticLevel.FULL, "maintenance thread stopping on node " + getKey());
            }
        }.start();
    }

    private void shutdownMaintenanceThread() {

        enablePredecessorMaintenance(false);
        enableStabilization(false);
        enablePeerStateMaintenance(false);
    }

    private boolean predecessorMaintenanceEnabled() {

        return predecessor_maintenance_enabled;
    }

    private boolean stabilizationEnabled() {

        return stabilization_enabled;
    }

    private boolean fingerTableMaintenanceEnabled() {

        return finger_table_maintenance_enabled;
    }
}