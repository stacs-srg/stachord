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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Observable;

import uk.ac.standrews.cs.nds.events.Event;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
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

    private InetSocketAddress local_address; // The address of this node.
    private final IKey key; // The key of this node.
    private final int hash_code; // The hash code of this node.
    private IChordRemoteReference self_reference; // A local RMI reference to this node.
    private IChordRemoteReference predecessor; // The predecessor of this node.
    private IChordRemoteReference successor; // The successor of this node.
    private final SuccessorList successor_list; // The successor list of this node.
    private final FingerTable finger_table; // The finger table of this node.
    private final ChordRemoteServer chord_remote_server;

    private final boolean own_address_maintenance_enabled = true; // Whether periodic checking of own address is enabled
    private boolean predecessor_maintenance_enabled = true; // Whether periodic predecessor maintenance should be performed.
    private boolean stabilization_enabled = true; // Whether periodic ring stabilization should be performed.
    private boolean finger_table_maintenance_enabled = true; // Whether periodic finger table maintenance should be performed.
    private boolean detailed_to_string = false; // Whether toString() should return a detailed description.

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new Chord node bound to the given local address, with a key generated by hashing the address.
     * @param local_address the local address
     * @throws IOException 
     */
    public ChordNodeImpl(final InetSocketAddress local_address) throws IOException {

        this(local_address, new SHA1KeyFactory().generateKey(local_address));
    }

    /**
     * Creates a new Chord node bound to the given local address, with the given key.
     * @param local_address the local address
     * @param key the key
     * @throws IOException 
     */
    public ChordNodeImpl(final InetSocketAddress local_address, final IKey key) throws IOException {

        this.local_address = local_address;
        this.key = key;
        hash_code = local_address.hashCode();
        successor_list = new SuccessorList(this);
        finger_table = new FingerTable(this);
        chord_remote_server = new ChordRemoteServer(this);

        initialiseSelfReference();

        createRing();
        exposeNode();

        startMaintenanceThread();
        addObserver(this);
    }

    private void initialiseSelfReference() {

        self_reference = new ChordRemoteReference(key, local_address);

    }

    private void initialiseSelfSuccessorReference() throws RemoteException {

        if (successorIsSelf()) {
            successor = self_reference;
        }
    }

    // -------------------------------------------------------------------------------------------------------

    // IChordNode operations.
    @Override
    public IKey getKey() {

        return key;
    }

    @Override
    public IChordRemoteReference lookup(final IKey k) throws RemoteException {

        if (k.equals(key) || successorIsSelf()) {

            // If the key is equal to this node's, or the ring currently only has one node...
            return self_reference;
        }
        return findSuccessor(k);
    }

    private boolean successorIsSelf() throws RemoteException {

        return successor.getCachedKey().equals(key);
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
            unexposeNode();
        }
        catch (final Exception e) {
            Diagnostic.trace(DiagnosticLevel.FULL, "failed to destroy node: ", key);
        }

        Diagnostic.trace(DiagnosticLevel.FULL, "shutdown node: ", key);
    }

    /**
     * Checks whether the given key lies in this node's key range.
     * @param k a key
     * @return true if the key lies in this node's key range
     * @throws RemoteException 
     */
    @Override
    public boolean inLocalKeyRange(final IKey k) throws RemoteException {

        // This is never called when the predecessor is null.
        return RingArithmetic.inHalfOpenSegment(k, predecessor.getCachedKey(), key);
    }

    // -------------------------------------------------------------------------------------------------------

    // IChordRemote operations.
    @Override
    public InetSocketAddress getAddress() {

        return local_address;
    }

    @Override
    public void notify(final IChordRemoteReference potential_predecessor) throws RemoteException {

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
        final IKey key_of_potential_predecessor = potential_predecessor.getCachedKey();

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
    public NextHopResult nextHop(final IKey k) throws RemoteException {

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
    public void notifyFailure(final IChordRemoteReference node) throws RemoteException {

        finger_table.fingerFailure(node);
    }

    @Override
    public String toStringDetailed() {

        final StringBuilder builder = new StringBuilder();
        builder.append("Node state\nkey: ");
        builder.append(key);
        builder.append("\nlocal_address: ");
        builder.append(local_address);
        builder.append("\npredecessor cached: ");
        builder.append(predecessor != null ? predecessor.getCachedAddress() : "null");

        builder.append("\npredecessor remote: ");
        try {
            builder.append(predecessor != null ? predecessor.getRemote().getAddress() : "null");
        }
        catch (final RemoteException e) {
            builder.append("\npredecessor remote unavailable ");
        }

        builder.append("\nsuccessor cached: ");
        builder.append(successor != null ? successor.getCachedAddress() : "null");

        builder.append("\nsuccessor remote: ");
        try {
            builder.append(successor != null ? successor.getRemote().getAddress() : "null");
        }
        catch (final RemoteException e) {
            builder.append("\nsuccessor remote unavailable ");
        }

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

        final Event event = (Event) arg;

        Diagnostic.traceNoSource(DiagnosticLevel.FULL, ">>>>>>>>>>>>>>>>>>>>>> update: " + event);

        if (event.equals(SUCCESSOR_CHANGE_EVENT)) {
            try {
                Diagnostic.trace(DiagnosticLevel.FULL, "successor now: ", (successor != null ? successor.getCachedKey() : "null"));
            }
            catch (final RemoteException e) {
                Diagnostic.trace(DiagnosticLevel.RUN, "Error handling successor change");
            }
        }

        if (event.equals(PREDECESSOR_CHANGE_EVENT)) {
            try {
                Diagnostic.trace(DiagnosticLevel.FULL, "predecessor now: ", (predecessor != null ? predecessor.getCachedKey() : "null"));
            }
            catch (final RemoteException e) {
                Diagnostic.trace(DiagnosticLevel.RUN, "Error handling predecessor change");
            }
        }

        if (event.equals(SUCCESSOR_LIST_CHANGE_EVENT)) {
            Diagnostic.trace(DiagnosticLevel.FULL, "successor list now: ", successor_list);
        }

        if (event.equals(FINGER_TABLE_CHANGE_EVENT)) {
            Diagnostic.trace(DiagnosticLevel.FULL, "finger table now: ", finger_table);
        }

        if (event.equals(OWN_ADDRESS_CHANGE_EVENT)) {
            Diagnostic.trace(DiagnosticLevel.FULL, "Address change event");
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Exposes this node for remote access.
     * @throws IOException 
     */
    private void exposeNode() throws IOException {

        chord_remote_server.setLocalAddress(local_address.getAddress());
        chord_remote_server.setPort(local_address.getPort());
        chord_remote_server.start();
    }

    private void unexposeNode() throws IOException {

        chord_remote_server.stop();
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
     * Executes the stabilization protocol.
     * @throws RemoteException 
     */
    private synchronized void stabilize() throws RemoteException {

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
     * Checks that the address of this node is as it was when it was created
     */
    private synchronized boolean checkNodeAddressChanged() {

        try {
            final InetAddress new_address = NetworkUtil.getLocalIPv4Address();

            final boolean address_has_changed = !new_address.equals(local_address.getAddress());

            if (address_has_changed) {
                System.out.println("Old address: " + local_address);
                local_address = new InetSocketAddress(new_address, local_address.getPort());
                System.out.println("New address: " + local_address);
            }
            return address_has_changed;
        }
        catch (final UnknownHostException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "couldn't find local address");
            return true;
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
     * @throws RemoteException 
     */
    private IChordRemoteReference closestPrecedingNode(final IKey k) throws RemoteException {

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
     * @throws RemoteException 
     */
    private void checkForBetterSuccessor(final IChordRemoteReference potential_successor) throws RemoteException {

        if (potential_successor != null) {

            final IKey key_of_potential_successor = potential_successor.getCachedKey();

            // Check whether the potential successor's key lies in this node's current successor's key range, and the potential successor is
            // not the current successor.
            if (inSuccessorKeyRange(key_of_potential_successor) && !key_of_potential_successor.equals(successor.getCachedKey())) {

                // The potential successor is more suitable as this node's successor.
                setSuccessor(potential_successor);
            }
        }
    }

    private void notifySuccessor() throws RemoteException {

        successor.getRemote().notify(self_reference);
    }

    private void refreshSuccessorList() throws RemoteException {

        if (!successor.getCachedKey().equals(getKey())) {
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

    private void handleAddressChange() throws IOException, RemoteException {

        unexposeNode();
        exposeNode();

        initialiseSelfReference();
        initialiseSelfSuccessorReference();

        // Try and rejoin the ring
        try {
            // first preference - rejoin the ring by binding to predecessor if we can
            if (predecessor == null) {
                try {
                    joinUsingFinger();
                }
                catch (final NoReachableNodeException e1) {
                    // we are on our own - nothing we can do 
                    // We don't have a predecessor and can't connect via the fingers
                    // In this case - will never rejoin without some manual intervention (?)
                    Diagnostic.trace("Cannot rejoin ring using predecessor (null) or finger");
                }
            }
            else {
                // normal case
                join(predecessor);
            }
        }
        catch (final RemoteException e) {
            // second preference - rejoin the ring by using a finger

            try {
                joinUsingFinger();
            }
            catch (final NoReachableNodeException e1) {
                // Not much else we can do here - for now
                // We will try and re-join if we get another adddress change event
                Diagnostic.trace("Cannot rejoin ring using predecessor or finger");
            }
        }
    }

    private void handleSuccessorError(final Exception e) throws RemoteException {

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
     * @param successor the new successor node
     */
    private synchronized void setSuccessor(final IChordRemoteReference successor) {

        assert successor != null;

        final IChordRemoteReference old_successor = this.successor;
        this.successor = successor;

        if (old_successor != null && !old_successor.equals(successor)) {
            setChanged();
            notifyObservers(SUCCESSOR_CHANGE_EVENT);
        }
    }

    private boolean inSuccessorKeyRange(final IKey k) throws RemoteException {

        return RingArithmetic.inHalfOpenSegment(k, key, successor.getCachedKey());
    }

    /**
     * Attempts to find a working successor from the successor list, or failing that using the predecessor or a finger.
     * @throws RemoteException 
     */
    private synchronized void findWorkingSuccessor() throws RemoteException {

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

    private void joinUsingFinger() throws NoReachableNodeException, RemoteException {

        for (final IChordRemoteReference finger : finger_table.getFingers()) {
            if (finger != null && !finger.getCachedKey().equals(key)) {
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

                while (predecessorMaintenanceEnabled() || stabilizationEnabled() || fingerTableMaintenanceEnabled() || ownAddressMaintenanceEnabled()) {

                    try {
                        sleep(WAIT_PERIOD);
                    }
                    catch (final InterruptedException e) {
                        // Ignore.
                    }

                    if (ownAddressMaintenanceEnabled()) {
                        if (checkNodeAddressChanged()) {
                            try {
                                handleAddressChange();
                            }
                            catch (final Exception e) {
                                Diagnostic.trace(DiagnosticLevel.RUN, "Error handling address change: " + e.getMessage());
                            }

                            setChanged();
                            notifyObservers(OWN_ADDRESS_CHANGE_EVENT);
                        }
                    }

                    if (predecessorMaintenanceEnabled()) {
                        checkPredecessor();
                    }

                    if (stabilizationEnabled()) {
                        try {
                            stabilize();
                        }
                        catch (final RemoteException e) {
                            Diagnostic.trace(DiagnosticLevel.RUN, "error in stabilize", e);
                        }
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

    private boolean ownAddressMaintenanceEnabled() {

        return own_address_maintenance_enabled;
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
