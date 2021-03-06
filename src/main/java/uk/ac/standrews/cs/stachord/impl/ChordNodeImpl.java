/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2011 Distributed Systems Architecture Research Group *
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

import uk.ac.standrews.cs.nds.events.Event;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.RingArithmetic;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.utilities.archive.Diagnostic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of Chord node.
 *
 * @author Stephanie Anderson
 * @author Stuart Norcross (stuart@cs.st-andrews.ac.uk)
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
class ChordNodeImpl extends Observable implements IChordNode, IChordRemote {

    private static final int PREDECESSOR_ERROR_ACTION_THRESHOLD = 1; // The number of predecessor errors that will be ignored before the predecessor is reset to null.
    private final IKey key; // The key of this node.
    private final int hash_code; // The hash code of this node.
    private final SuccessorList successor_list; // The successor list of this node.
    private final FingerTable finger_table; // The finger table of this node.
    private final ChordRemoteServer chord_remote_server;
    private final ChordMaintenanceThread maintenance_thread;
    private final boolean own_address_maintenance_enabled = true; // Whether periodic checking of own address is enabled
    private volatile InetSocketAddress local_address; // The address of this node.
    private volatile IChordRemoteReference self_reference; // A local reference to this node.
    private volatile IChordRemoteReference predecessor; // The predecessor of this node.
    private volatile IChordRemoteReference successor; // The successor of this node.
    private volatile boolean predecessor_maintenance_enabled = true; // Whether periodic predecessor maintenance should be performed.
    private volatile boolean stabilization_enabled = true; // Whether periodic ring stabilization should be performed.
    private volatile boolean finger_table_maintenance_enabled = true; // Whether periodic finger table maintenance should be performed.
    private volatile boolean detailed_to_string = false; // Whether toString() should return a detailed description.
    private volatile int predecessor_error_count = 0;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new Chord node bound to the given local address, with a key generated by hashing the address.
     *
     * @param local_address the local address
     * @throws IOException if the service cannot bind to the specified local address
     */
    public ChordNodeImpl(final InetSocketAddress local_address) throws IOException {

        this(local_address, new SHA1KeyFactory().generateKey(local_address));
    }

    /**
     * Creates a new Chord node bound to the given local address, with the given key.
     *
     * @param local_address the local address
     * @param key           the key
     * @throws IOException if the service cannot bind to the specified local address
     */
    public ChordNodeImpl(final InetSocketAddress local_address, final IKey key) throws IOException {

        this.local_address = local_address;
        this.key = key;
        hash_code = local_address.hashCode();
        successor_list = new SuccessorList(this);
        finger_table = new FingerTable(this);
        chord_remote_server = new ChordRemoteServer(this);
        maintenance_thread = new ChordMaintenanceThread(this);

        exposeNode();
        initialiseSelfReference();
        createRing();

        startMaintenanceThread();
        addObserver(this);
    }

    // -------------------------------------------------------------------------------------------------------

    // Operations that use synchronization.

    /**
     * Checks whether the given key lies in this node's key range.
     *
     * @param k a key
     * @return true if the key lies in this node's key range
     * @throws RPCException if an error occurs in accessing this node's predecessor's key
     */
    @Override
    public boolean inLocalKeyRange(final IKey k) throws RPCException {

        // It's possible that predecessor and successor will change during execution of this method, leading to transiently
        // incorrect results. We don't care about this, so only synchronize enough of the method to avoid NPEs.

        final IKey predecessor_key;

        synchronized (this) {
            // getCachedKey() is a non-open call, holding lock on this node.
            // It may make a remote getKey() call on the predecessor, which doesn't require any further locks.
            predecessor_key = predecessor != null ? predecessor.getCachedKey() : null;
        }

        if (predecessor_key == null) {
            if (successorIsSelf()) {
                return true;
            }

            // No predecessor and successor not self, so not a one-node ring - don't know local key range.
            throw new KeyUnknownException("Unable to determine local key range because the predecessor is null. This is not a JSON RPCException.");
        }

        return RingArithmetic.inSegment(predecessor_key, k, key);
    }

    @Override
    public IKey getKey() {

        return key;
    }

    // -------------------------------------------------------------------------------------------------------

    // IChordNode operations.

    @Override
    public IChordRemoteReference lookup(final IKey k) throws RPCException {

        if (inLocalKeyRange(k)) {

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
    public IChordRemoteReference getSelfReference() {

        return self_reference;
    }

    @Override
    public void shutDown() {

        shutdownMaintenanceThread();

        // Shutdown the server.
        try {
            unexposeNode();
        } catch (final Exception e) {
            Diagnostic.trace("failed to destroy node: " + key);
        }
    }

    @Override
    public InetSocketAddress getAddress() {

        return local_address;
    }

    // -------------------------------------------------------------------------------------------------------

    // IChordRemote operations.

    @Override
    public void join(final IChordRemoteReference known_node) throws RPCException {

        // Route to this node's key; the result is this node's new successor.
        final IChordRemote remote = known_node.getRemote();
        final IChordRemoteReference new_successor = remote.lookup(key);

        // Check that the new successor is not this node. This could happen if this node is already in a ring containing the known node.
        // This could happen in a situation where we're trying to combine two rings by having in a node in one join using a node in the
        // other as the known node, but where they're actually in the same ring. Perhaps unlikely, but we can never be completely sure
        // whether a ring has partitioned or not.
        if (!equals(new_successor.getRemote())) {
            setSuccessor(new_successor);
        }
    }

    @Override
    public void notify(final IChordRemoteReference potential_predecessor) throws RPCException {

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
    public NextHopResult nextHop(final IKey k) throws RPCException {

        // Check whether the key lies in this node's successor's key range, in which case the successor represents the final hop.
        if (inSuccessorKeyRange(k)) {
            return new NextHopResult(successor, true);
        }

        final IChordRemoteReference closest_preceding_node = closestPrecedingNode(k);

        // It's the final hop if the node's key is equal to the target key.
        final boolean final_hop = closest_preceding_node.getCachedKey().equals(k);
        return new NextHopResult(closest_preceding_node, final_hop);
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
    public void notifyFailure(final IChordRemoteReference node) throws RPCException {

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
        } catch (final RPCException e) {
            builder.append("\npredecessor remote unavailable ");
        }

        builder.append("\nsuccessor cached: ");
        builder.append(successor != null ? successor.getCachedAddress() : "null");

        builder.append("\nsuccessor remote: ");
        try {
            builder.append(successor != null ? successor.getRemote().getAddress() : "null");
        } catch (final RPCException e) {
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

    @Override
    public int hashCode() {

        return hash_code;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {

        return detailed_to_string ? toStringDetailed() : toStringTerse();
    }

    @Override
    public boolean equals(final Object other) {

        try {
            return other instanceof IChordRemote && ((IChordRemote) other).getKey().equals(key);
        } catch (final RPCException e) {
            return false;
        }
    }

    public void showState() {

        System.out.println(toStringDetailed());
    }

    public void setToStringDetailed(final boolean detailed) {

        detailed_to_string = detailed;
    }

    @Override
    public void update(final Observable o, final Object arg) {

        final Event event = (Event) arg;

        Diagnostic.traceNoSource(">>>>>>>>>>>>>>>>>>>>>> update: " + event);

        if (event.equals(SUCCESSOR_CHANGE_EVENT)) {
            try {
                Diagnostic.trace("successor of " + key + " now: " + (successor != null ? successor.getCachedKey() : "null"));
            } catch (final RPCException e) {
                Diagnostic.trace("Error handling successor change");
            }
        }

        if (event.equals(PREDECESSOR_CHANGE_EVENT)) {
            try {
                Diagnostic.trace("\n\npredecessor of " + key + " now: " + (predecessor != null ? predecessor.getCachedKey() : "null"));
            } catch (final RPCException e) {
                Diagnostic.trace("Error handling predecessor change");
            }
        }

        if (event.equals(SUCCESSOR_LIST_CHANGE_EVENT)) {
            Diagnostic.trace("successor list now: " + successor_list);
        }

        if (event.equals(FINGER_TABLE_CHANGE_EVENT)) {
            Diagnostic.trace("finger table now: " + finger_table);
        }

        if (event.equals(OWN_ADDRESS_CHANGE_EVENT)) {
            Diagnostic.trace("Address change event");
        }
    }

    // -------------------------------------------------------------------------------------------------------

    private boolean inSuccessorKeyRange(final IKey k) throws RPCException {

        final IKey successor_key;

        synchronized (this) {
            successor_key = successor != null ? successor.getCachedKey() : null;
        }

        if (successor_key == null) {
            throw new KeyUnknownException("Unable to determine successor key range because the successor is null. This is not a JSON RPCException.");
        }

        return RingArithmetic.inSegment(key, k, successor_key);
    }

    @Override
    protected void setChanged() {

        super.setChanged();
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Exposes this node for remote access.
     *
     * @throws IOException                  if the service cannot bind to the specified local address
     * @throws RPCException                 if an error occurs binding the application to the registry
     * @throws AlreadyBoundException        if another instance of the application is already bound in the registry
     * @throws RegistryUnavailableException if the registry is unavailable
     * @throws TimeoutException
     * @throws InterruptedException
     */
    void exposeNode() throws IOException {

        chord_remote_server.setLocalAddress(local_address.getAddress());
        chord_remote_server.setPort(local_address.getPort());

        // If the port of another Chord node is already bound in the registry, just overwrite it, don't throw exception.
        chord_remote_server.startWithNoRegistry();
        local_address = chord_remote_server.getAddress();
    }

    void unexposeNode() throws IOException {

        chord_remote_server.stop();
    }

    /**
     * Sets a new predecessor for this node.
     *
     * @param new_predecessor the new predecessor
     */
    void setPredecessor(final IChordRemoteReference new_predecessor) {

        final IChordRemoteReference old_predecessor = predecessor;
        predecessor = new_predecessor;

        if (new_predecessor != null) {
            predecessor_error_count = 0;
        }

        if (new_predecessor == null || !new_predecessor.equals(old_predecessor)) {

            setChanged();
            notifyObservers(PREDECESSOR_CHANGE_EVENT);
        }
    }

    /**
     * Sets the successor node in key space.
     *
     * @param successor the new successor node
     */
    void setSuccessor(final IChordRemoteReference successor) {

        assert successor != null;

        final IChordRemoteReference old_successor = this.successor;
        this.successor = successor;

        if (old_successor != null && !old_successor.equals(successor)) {

            setChanged();
            notifyObservers(SUCCESSOR_CHANGE_EVENT);
        }
    }

    /**
     * Checks whether this node's successor is itself, i.e. whether it is in a one-node ring.
     *
     * @return true if this node's successor is itself
     * @throws RPCException if an error occurs accessing the successor's key
     */
    boolean successorIsSelf() throws RPCException {

        return successor.getCachedKey().equals(key);
    }

    void initialiseSelfReference() {

        self_reference = new ChordRemoteReference(key, local_address);
    }

    SuccessorList getRealSuccessorList() {

        return successor_list;
    }

    FingerTable getFingerTable() {

        return finger_table;
    }

    boolean ownAddressMaintenanceEnabled() {

        return own_address_maintenance_enabled;
    }

    boolean predecessorMaintenanceEnabled() {

        return predecessor_maintenance_enabled;
    }

    boolean stabilizationEnabled() {

        return stabilization_enabled;
    }

    boolean fingerTableMaintenanceEnabled() {

        return finger_table_maintenance_enabled;
    }

    void setAddress(final InetSocketAddress local_address) {

        this.local_address = local_address;
    }

    void handlePredecessorError() {

        predecessor_error_count++;

        if (predecessor_error_count > PREDECESSOR_ERROR_ACTION_THRESHOLD) {
            Diagnostic.trace("resetting predecessor after " + predecessor_error_count + " errors");
            setPredecessor(null);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Sets data structures for a new ring.
     */
    private void createRing() {

        setPredecessor(null);
        setSuccessor(self_reference);
        successor_list.clear();
        setChanged();
        notifyObservers(SUCCESSOR_LIST_CHANGE_EVENT);
    }

    /**
     * Returns the closest preceding node from the finger table, or the successor if there is no such node in the finger table.
     *
     * @param k a key
     * @return the peer node whose key most closely precedes k
     * @throws RemoteChordException
     */
    private IChordRemoteReference closestPrecedingNode(final IKey k) throws RPCException {

        try {
            return finger_table.closestPrecedingNode(k);
        } catch (final NoPrecedingNodeException e) {
            return successor;
        }
    }

    /**
     * Precondition: key is not in local key range.
     */
    private IChordRemoteReference findSuccessor(final IKey key) throws RPCException {

        assert !inLocalKeyRange(key);

        // Get the first hop.
        NextHopResult next_hop = nextHop(key);

        // Keep track of the hop before the next one, in case the next one turns out to have failed
        // and the one before it has to be notified so it can update its finger table.
        IChordRemote current_hop = this;

        while (!next_hop.isFinalHop()) {
            try {
                // Next hop mustn't be this node, or further from us than the target.
                assert !this.key.equals(next_hop.getNode().getCachedKey());
                assert !RingArithmetic.ringDistanceFurther(this.key, next_hop.getNode().getCachedKey(), key);

                // Remember the previous value of next_hop.
                final IChordRemote previous_next_hop = next_hop.getNode().getRemote();

                next_hop = previous_next_hop.nextHop(key);

                current_hop = previous_next_hop;
            } catch (final RPCException e) {
                current_hop.notifyFailure(next_hop.getNode());
                throw new RPCException("hop failure on node " + local_address + " trying to contact node " + next_hop.getNode().getCachedAddress(), e);
            } catch (final RuntimeException e) {
                current_hop.notifyFailure(next_hop.getNode());
                throw e;
            }
        }
        return next_hop.getNode();
    }

    private void startMaintenanceThread() {

        maintenance_thread.start();
    }

    private void shutdownMaintenanceThread() {

        maintenance_thread.shutdown();
    }
}
