package uk.ac.standrews.cs.stachord.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.RingArithmetic;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

final class ChordMaintenanceThread extends Thread {

    private static final Duration MAINTENANCE_WAIT_INTERVAL = new Duration(3, TimeUnit.SECONDS);

    private final ChordNodeImpl chord_node;

    ChordMaintenanceThread(final ChordNodeImpl chord_node) {

        this.chord_node = chord_node;
    }

    @Override
    public void run() {

        while (!isInterrupted()) {

            if (chord_node.ownAddressMaintenanceEnabled()) {
                checkOwnAddress();
            }

            if (chord_node.predecessorMaintenanceEnabled()) {
                checkPredecessor();
            }

            if (chord_node.stabilizationEnabled()) {
                stabilize();
            }

            if (chord_node.fingerTableMaintenanceEnabled()) {
                fixNextFinger();
            }

            MAINTENANCE_WAIT_INTERVAL.sleep();
        }

        Diagnostic.trace(DiagnosticLevel.RUN, "maintenance thread stopping on node " + chord_node.getKey());
    }

    protected void shutdown() {

        interrupt();
    }

    // -------------------------------------------------------------------------------------------------------

    /**
      * Checks whether the local address has changed, for example due to switching NIC or moving behind a NAT.
      */
    private void checkOwnAddress() {

        if (checkNodeAddressChanged()) {
            try {
                handleAddressChange();
            }
            catch (final Exception e) {
                Diagnostic.trace(DiagnosticLevel.RUN, "Error handling address change: " + e.getMessage());
            }

            chord_node.setChanged();
            chord_node.notifyObservers(IChordNode.OWN_ADDRESS_CHANGE_EVENT);
        }
    }

    /**
     * Checks whether the address of this node has changed since the last check.
     */
    private boolean checkNodeAddressChanged() {

        try {
            final InetAddress current_address = NetworkUtil.getLocalIPv4Address();

            final InetSocketAddress previous_socket_address = chord_node.getAddress();
            final boolean address_has_changed = !current_address.equals(previous_socket_address.getAddress());

            if (address_has_changed) {

                Diagnostic.trace(DiagnosticLevel.RUN, "Address change: old : " + previous_socket_address);
                final InetSocketAddress new_socket_address = new InetSocketAddress(current_address, previous_socket_address.getPort());
                chord_node.setAddress(new_socket_address);
                Diagnostic.trace(DiagnosticLevel.RUN, "New: " + new_socket_address);
            }

            return address_has_changed;
        }
        catch (final UnknownHostException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "couldn't find local address");
            return true;
        }
    }

    private void handleAddressChange() throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException {

        chord_node.unexposeNode();
        chord_node.exposeNode();

        chord_node.initialiseSelfReference();
        initialiseSelfSuccessorReference();

        // Try to rejoin the ring.
        try {
            // First preference: rejoin the ring by binding to predecessor if we can.
            final IChordRemoteReference predecessor = chord_node.getPredecessor();
            if (predecessor == null) {
                try {
                    joinUsingFinger();
                }
                catch (final NoReachableNodeException e1) {

                    // We don't have a predecessor and can't connect via the fingers
                    // In this case - will never rejoin without some manual intervention (?)
                    Diagnostic.trace("Cannot rejoin ring using predecessor (null) or finger");
                }
            }
            else {
                // Normal case
                chord_node.join(predecessor);
            }
        }
        catch (final Exception e) {

            // Second preference: rejoin the ring by using a finger.

            try {
                joinUsingFinger();
            }
            catch (final NoReachableNodeException e1) {

                // Not much else we can do here.
                // We will try and re-join if we get another address change event.
                Diagnostic.trace("Cannot rejoin ring using predecessor or finger");
            }
        }
    }

    private void initialiseSelfSuccessorReference() throws RPCException {

        if (chord_node.successorIsSelf()) {
            chord_node.setSuccessor(chord_node.getSelfReference());
        }
    }

    /**
     * Tries to communicate with this node's predecessor.
     */
    private void checkPredecessor() {

        try {
            pingPredecessor();
        }
        catch (final Exception e) {

            chord_node.handlePredecessorError();
        }
    }

    /**
     * Attempts to communicate with this node's predecessor.
     * @throws RPCException if the attempted communication fails
     */
    private void pingPredecessor() throws RPCException {

        final IChordRemoteReference predecessor = chord_node.getPredecessor();
        if (predecessor != null) {
            predecessor.ping();
        }
    }

    /**
     * Executes the stabilization protocol.
     */
    private void stabilize() {

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
        catch (final Exception e) {

            try {
                // Error contacting successor.
                handleSuccessorError();
            }
            catch (final RPCException e1) {
                Diagnostic.trace(DiagnosticLevel.RUN, "error in stabilize", e1);
            }
        }
    }

    /**
     * Checks whether a given potential successor would be a better successor for this node than the current successor.
     * @param potential_successor the potential successor
     * @throws RPCException
     */
    private void checkForBetterSuccessor(final IChordRemoteReference potential_successor) throws RPCException {

        if (potential_successor != null) {

            final IChordRemoteReference successor = chord_node.getSuccessor();
            final IKey key_of_potential_successor = potential_successor.getCachedKey();

            // Check whether the potential successor's key lies in this node's current successor's key range, and the potential successor is not the current successor.
            if (RingArithmetic.inSegment(chord_node.getKey(), key_of_potential_successor, successor.getCachedKey()) && !key_of_potential_successor.equals(successor.getCachedKey())) {

                // The potential successor is more suitable as this node's successor.
                chord_node.setSuccessor(potential_successor);
            }
        }
    }

    private void notifySuccessor() throws RPCException {

        assert chord_node.getSelfReference().getCachedAddress().equals(chord_node.getAddress());

        final IChordRemoteReference successor = chord_node.getSuccessor();
        final IChordRemoteReference selfReference = chord_node.getSelfReference();

        //        System.out.println("node at " + selfReference.getCachedAddress() + " notifying self to " + successor.getCachedAddress());

        successor.getRemote().notify(selfReference);
    }

    private void refreshSuccessorList() throws RPCException {

        final IChordRemoteReference successor = chord_node.getSuccessor();

        if (!successor.getCachedKey().equals(chord_node.getKey())) {
            try {
                final List<IChordRemoteReference> successor_list_of_successor = successor.getRemote().getSuccessorList();

                if (chord_node.getRealSuccessorList().refreshList(successor_list_of_successor)) {
                    chord_node.setChanged();
                    chord_node.notifyObservers(IChordNode.SUCCESSOR_LIST_CHANGE_EVENT);
                }
            }
            catch (final Exception e) {
                Diagnostic.trace(DiagnosticLevel.FULL, chord_node, ": error calling successor ", chord_node.getSuccessor(), ": ", e);
                handleSuccessorError();
            }
        }
    }

    /**
     * Attempts to find a working successor from the successor list, or failing that using the predecessor or a finger.
     * @throws RPCException
     */
    private void handleSuccessorError() throws RPCException {

        try {
            chord_node.setSuccessor(chord_node.getRealSuccessorList().findFirstWorkingNode());
        }
        catch (final NoReachableNodeException e) {
            try {
                chord_node.join(chord_node.getPredecessor());
            }
            catch (final Exception e1) {

                // RPCException if predecessor has failed, or NullPointerException if it's already null
                try {
                    joinUsingFinger();
                }
                catch (final NoReachableNodeException e2) {

                    // Couldn't contact any known node in current ring.
                    // Leave things as they are, in the hope that the current successor will recover.
                }
            }
        }
    }

    /**
      * Checks the next finger, and updates the finger table entry if a better finger is available.
      */
    private void fixNextFinger() {

        if (chord_node.getFingerTable().fixNextFinger()) {
            chord_node.setChanged();
            chord_node.notifyObservers(IChordNode.FINGER_TABLE_CHANGE_EVENT);
        }
    }

    /**
     * Gets the predecessor of this node's successor.
     * @return the predecessor of this node's successor
     * @throws RPCException if an error occurs communicating with the successor
     */
    private IChordRemoteReference getPredecessorOfSuccessor() throws RPCException {

        return chord_node.getSuccessor().getRemote().getPredecessor();
    }

    private void joinUsingFinger() throws NoReachableNodeException, RPCException {

        for (final IChordRemoteReference finger : chord_node.getFingerTable().getFingers()) {
            if (finger != null && !finger.getCachedKey().equals(chord_node.getKey())) {
                try {
                    chord_node.join(finger);
                    return;
                }
                catch (final RPCException e) {
                    // Ignore and try next finger.
                }
            }
        }
        throw new NoReachableNodeException();
    }
}
