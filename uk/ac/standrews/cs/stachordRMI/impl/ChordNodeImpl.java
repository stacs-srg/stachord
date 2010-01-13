/*
 *  StAChord Library
 *  Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 *  http://asa.cs.st-andrews.ac.uk/
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.stachordRMI.impl;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import uk.ac.standrews.cs.nds.eventModel.Event;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.exceptions.SimulatedFailureException;
import uk.ac.standrews.cs.nds.p2p.impl.AID;
import uk.ac.standrews.cs.nds.p2p.impl.P2PStatus;
import uk.ac.standrews.cs.nds.p2p.interfaces.IApplicationRegistry;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PNode;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.Pair;
import uk.ac.standrews.cs.stachordRMI.factories.GeometricFingerTableFactory;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoPrecedingNodeException;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoReachableNodeException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTable;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTableFactory;
import uk.ac.standrews.cs.stachordRMI.util.SegmentArithmetic;

/**
 * Implementation of Chord node.
 * 
 * @author sja7, stuart, al, graham
 */
public class ChordNodeImpl extends Observable implements IChordNode, Remote  {

	private InetSocketAddress local_address;
	private IKey key;
	private int hash_code;

	private IChordRemoteReference predecessor, successor;
	private IChordRemoteReference proxy; // overkill but keeps code tidy.
	private SuccessorList successor_list;
	private IFingerTable finger_table;

	private IApplicationRegistry registry;
	private boolean simulating_failure;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new Chord node using a geometric finger table.
	 * 
	 * @param local_address the network address of the node
	 * @param key the node's key
	 * @param bus an event bus
	 */
	public ChordNodeImpl(InetSocketAddress local_address, IKey key ) {
		this(local_address, key, new GeometricFingerTableFactory());
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Default constructor used in deserialisation.
	 */
	protected ChordNodeImpl() {
		// Deliberately empty.
	}

	public ChordNodeImpl(InetSocketAddress local_address, IKey key, IFingerTableFactory fingerTableFactory ) {

		this.local_address = local_address;
		this.key = key;

		hash_code = hashCode();

		predecessor = null;
		successor = null;
		successor_list = new SuccessorList(this);

		proxy = new ChordRemoteReference( key, new ChordNodeProxy( this ) );

		finger_table = fingerTableFactory.makeFingerTable(this);

		simulating_failure = false;

		Diagnostic.trace(DiagnosticLevel.INIT, "initialised with key: ", key);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// INetworkNodeRepresentation

	public InetSocketAddress getAddress() {

		return local_address;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IP2PNodeRepresentation

	public IKey getKey() {

		// This method is RAFDA-cached in remote references, so a call to a remote instance should never fail.
		return key;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Comparable<IP2PNode>

	public int compareTo(IP2PNode other) {

		if (other == null) return 1;

		return key.compareTo(other.getKey());
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IP2PNode

	public IChordRemoteReference lookup(IKey k) throws RemoteException {
		checkSimulatedFailure();

		/* If the key specified is greater than this node's key, and less than or equal
		 * to this node's successor's key, return this node's successor.
		 * Else iteratively call findSuccessor on nodes preceding the given
		 * key, until a suitable successor for the key is found (when the
		 * key is greater than the current node's key, and less than or
		 * equal to the current node's successor's key).
		 */

		long start_time = System.currentTimeMillis();

		if (k.equals(key) || successor.getKey().equals( this.getKey() ) ) {
			return proxy;
		}

		// If the key lies between this node and its successor, return the successor.
		if (inSuccessorKeyRange(k)) {
			return successor;
		}

		try {
			return findNonLocalSuccessor(k, start_time);
		} catch (P2PNodeException e) {
			throw new RemoteException();
		}
	}

	public int routingStateSize() {

		return finger_table.size();
	}

	public boolean isSimulatingFailure() {

		return simulating_failure;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IChordRemote

	public void notify(IChordRemoteReference potential_predecessor) {

		checkSimulatedFailure();

		// Only use the suggested predecessor if it's in the current key range.
		// Also check that the suggested predecessor is not this node, for paranoia.

		try {
			if (inLocalKeyRange(potential_predecessor.getKey()) && !equals(potential_predecessor)) {
				setPredecessor(potential_predecessor);
			}
		}
		catch (P2PNodeException e) {
			// There isn't currently a predecessor, so use the suggested one.
			setPredecessor(potential_predecessor);
		}

	}

	public ArrayList<IChordRemoteReference> getSuccessorList() {

		checkSimulatedFailure();
		return successor_list.getList();
	}

	public ArrayList<IChordRemoteReference> getFingerList() {

		checkSimulatedFailure();
		return finger_table.getFingers();
	}

	public IChordRemoteReference getPredecessor() {

		checkSimulatedFailure();
		return predecessor;
	}

	public IChordRemoteReference getSuccessor() {

		checkSimulatedFailure();
		return successor;
	}

	public void isAlive() {

		checkSimulatedFailure();
	}

	public Pair<NextHopResultStatus, IChordRemoteReference> nextHop(IKey k) {

		checkSimulatedFailure();

		// Check whether the key lies in the range between this node and its successor,
		// in which case the successor represents the final hop.

		if (inSuccessorKeyRange(k)) return new Pair<NextHopResultStatus, IChordRemoteReference>(NextHopResultStatus.FINAL, successor);
		else                        return new Pair<NextHopResultStatus, IChordRemoteReference>(NextHopResultStatus.NEXT_HOP, closestPrecedingNode(k));
	}

	public Object locateApplicationComponent(IKey k, AID application_id) throws P2PNodeException {

		// Perhaps this method should be synchronized - the predecessor field
		// can be set to null during the execution of this method.

		checkSimulatedFailure();

		// Check that this node is the root node for k.
		try {
			if (inLocalKeyRange(k))
				return registry.locateApplicationComponent(k, application_id);
			else
				throw new P2PNodeException(P2PStatus.LOOKUP_FAILURE, "this node (" + getKey() + ") is not the root for the specified key (" + k + ")");
		}
		catch (P2PNodeException e) {
			throw new P2PNodeException(P2PStatus.LOOKUP_FAILURE, "cannot resolve the root node for the specified key (" + k + ") because this node's routing state is currently incomplete. Predecessor is currently null.");
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IChordNode

	public synchronized void createRing() {

		checkSimulatedFailure();

		setPredecessor(null);
		setSuccessor(this.proxy);

		successor_list.refreshList();
	}

	public synchronized boolean join(IChordRemoteReference known_node) throws RemoteException { 
		checkSimulatedFailure();
		IChordRemoteReference initial_successor;
		initial_successor = known_node.getRemote().lookup(key);
		setSuccessor(initial_successor);
		return true;
	}

	public synchronized void stabilize() {

		checkSimulatedFailure();

		try {
			// Find predecessor of this node's successor.
			IChordRemoteReference predecessor_of_successor = getPredecessorOfSuccessor();

			// Check whether that is a better successor for this node than its current successor.
			// This may update this node's successor pointer.
			boolean found_better_successor = checkForBetterSuccessor(predecessor_of_successor);

			// Notify this node's successor (which may have just changed) that this node may be its predecessor.
			notifySuccessor();

			// Update this node's successor list from its successor's.
			boolean found_changes_in_successor_list = refreshSuccessorList();

		}
		catch (Exception e) {
			//Error contacting successor.
			handleSuccessorError(e);
		}
	}

	public synchronized void checkPredecessor() {

		checkSimulatedFailure();

		try {
			pingPredecessor();
		}
		catch (Exception e) {
			//			suggestSuspectedFailure(predecessor);
			setPredecessor(null);
		}
	}

	public synchronized void fixNextFinger() {
		checkSimulatedFailure();
		finger_table.fixNextFinger();
	}

	public synchronized void fixAllFingers() {
		checkSimulatedFailure();
		finger_table.fixAllFingers();
	}

	public synchronized void setPredecessor(IChordRemoteReference new_predecessor) {
		checkSimulatedFailure();

		IChordRemoteReference oldPredecessor = predecessor;

		predecessor = new_predecessor;

		if (oldPredecessor != null && !oldPredecessor.equals(new_predecessor)){
			setChanged();
			notifyObservers();
		}
	}

	public IFingerTable getFingerTable() {

		checkSimulatedFailure();

		return finger_table;
	}

	public boolean inLocalKeyRange(IKey k) throws P2PNodeException {

		checkSimulatedFailure();

		// Need the predecessor key to test whether k lies within range.
		if (predecessor == null) {
			throw new P2PNodeException(P2PStatus.STATE_ACCESS_FAILURE, "predecessor is null");
		}
		return SegmentArithmetic.inHalfOpenSegment(k, predecessor.getKey(), key);
	}

	public void setSimulatingFailure(boolean simulating_failure) {

		this.simulating_failure = simulating_failure;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public int hashCode() {

		// This method is RAFDA-cached in remote references, so a call to a remote instance should never fail.
		return hash_code;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Returns the closest preceding node from the finger table, or the successor if there
	 * is no such node in the finger table
	 * 
	 * @param k a key
	 * @return the peer node the closest preceding key to k
	 */
	private IChordRemoteReference closestPrecedingNode(IKey k) {

		try {
			return finger_table.closestPrecedingNode(k);
		}
		catch (NoPrecedingNodeException e) {
			return successor;
		}
	}

	private void pingPredecessor() throws RemoteException {

		if (predecessor != null) {
			// Try to communicate with predecessor.
			predecessor.getRemote().isAlive();
		}
	}

	private IChordRemoteReference getPredecessorOfSuccessor() {

		IChordRemoteReference predecessor_of_successor = null;
		try {
			predecessor_of_successor = successor.getRemote().getPredecessor();
		} catch (RemoteException e) {
			handleSuccessorError( e );
		}
		return predecessor_of_successor;
	}

	private boolean checkForBetterSuccessor(IChordRemoteReference predecessor_of_successor) {

		if (predecessor_of_successor != null) {

			IKey key_of_predecessor_of_successor;
			key_of_predecessor_of_successor = predecessor_of_successor.getKey();

			if (inSuccessorKeyRange(key_of_predecessor_of_successor)) {

				// The successor's predecessor is more suitable as this node's successor.
				setSuccessor(predecessor_of_successor);

				return true;
			}
		}
		return false;
	}

	private void notifySuccessor() {

		try {
			successor.getRemote().notify(this.proxy);
		} catch (RemoteException e) {
			handleSuccessorError( e );
		}

	}

	private boolean refreshSuccessorList() {

		if ( ! successor.getKey().equals( this.getKey() ) ) {

			List<IChordRemoteReference> successor_list_of_successor;
			try {
				successor_list_of_successor = successor.getRemote().getSuccessorList();
				return successor_list.refreshList(successor_list_of_successor);
			} catch (RemoteException e) {
				handleSuccessorError( e );
			}
		}
		return false;
	}

	private void handleSuccessorError(Exception e) {
		Diagnostic.trace(DiagnosticLevel.FULL, this, ": error calling successor ", successor, ": ", e);

		findWorkingSuccessor();
	}

	private IChordRemoteReference findNonLocalSuccessor(IKey k, long start_time)  throws P2PNodeException {

		// Get the first hop.
		IChordRemoteReference next = closestPrecedingNode(k);
		int hop_count = 0;


		while (true) {

			Pair<NextHopResultStatus, IChordRemoteReference> result = getNextHop(k, next, hop_count);

			switch (result.first) {

			case NEXT_HOP: {
				next = result.second;
				break;
			}

			case FINAL: {
				next = result.second;
				return next;
			}

			default: {

				ErrorHandling.hardError("nextHop call returned NextHopResult with unrecognised code");
			}
			}

			hop_count++;
		}
	}

	private Pair<NextHopResultStatus, IChordRemoteReference> getNextHop(IKey k, IChordRemoteReference next, int hop_count) throws P2PNodeException {

		try {
			// Get the next hop.
			return next.getRemote().nextHop(k);
		}
		catch (Exception e) {

			// Gather events to compute the finger table access error rate.

			try { 

				Diagnostic.trace(DiagnosticLevel.RUN, this, ": signalling suspected failure of ", NetworkUtil.formatHostAddress(next.getRemote().getAddress()));

				// Only the first hop in the chain is to a finger.
				if (hop_count == 0) {
					suggestSuspectedFingerFailure(next);
				} else {
					//					suggestSuspectedFailure(next);
				}

				throw new P2PNodeException(P2PStatus.LOOKUP_FAILURE, "a failure ocurred when trying to determine the successor for key " + k + ": " + e.getMessage());
			}
			catch( RemoteException e1 ) {
				throw new P2PNodeException(P2PStatus.LOOKUP_FAILURE, "Multiple failures ocurred when trying to determine the successor" );

			}
		}
	}

	/**
	 * Sets the successor node in key space.
	 * 
	 * @param successor the new successor node
	 */
	private synchronized void setSuccessor(IChordRemoteReference successor) {	
		this.successor = successor;		
		setChanged();

		notifyObservers( new Event("SuccessorStateEvent") );
	}

	private boolean inSuccessorKeyRange(IKey k) {

		return SegmentArithmetic.inHalfOpenSegment(k, key, successor.getKey());
	}

	private void checkSimulatedFailure() {

		if (simulating_failure)
			throw new SimulatedFailureException("simulated failure");
	}

	/**
	 * Attempts to find a working successor from the successor list, or failing that using the predecessor or a finger.
	 */
	private synchronized void findWorkingSuccessor() {

		//		suggestSuspectedFailure(successor);

		try {
			IChordRemoteReference new_successor = successor_list.findFirstWorkingNode();
			setSuccessor(new_successor);
		}
		catch (NoReachableNodeException e) {

			try {
				join(predecessor);
			}
			catch (Exception e1) {

				try {
					joinUsingFinger();
				}
				catch (Exception e2) {

					// Couldn't contact any known node in current ring, so admit defeat and partition ring.
					createRing();
				}
			}
		}
	}

	private void joinUsingFinger() throws RemoteException {

		for (IChordRemoteReference node : finger_table.getFingers()) {
			join(node);
			return;
		}
	}

	//	/**
	//	 * Computes the time that has elapsed since the given start time.
	//	 * 
	//	 * @param start_time a time stamp as returned by System.currentTimeMillis())
	//	 * @return the elapsed time
	//	 */
	//	private int elapsedTime(long start_time) {
	//
	//		return (int) ((int) System.currentTimeMillis() - start_time);
	//	}
	//
	//	private void suggestSuspectedFailure(IChordRemoteReference node) {
	//
	//		Diagnostic.trace(DiagnosticLevel.FULL, this,": signalling suspected failure of ", node.getKey()); // TODO al and stuart are here
	//		generateEvent(ChordEventFactory.makeNodeFailureNotificationRepEvent(node.getKey())); 
	//		
	//	}

	private void suggestSuspectedFingerFailure(IChordRemoteReference node) {

		// Notify finger table that the node may have failed.
		finger_table.notifySuspectedFailure(node);

		// suggestSuspectedFailure(node);
	}

	public IChordRemoteReference getProxy() {
		return proxy;
	}

	@Override
	public String toString() {
		return "key: " + key + "" + " " + "local_address: " + local_address;
	}

	public String toStringFull() {
		return
		"Node state" + "\n" + 
		"key: " + key + "\n" + 
		"local_address: " + local_address + "\n" + 
		"predecessor: " + predecessor + "\n" + 
		"successor: " + successor + "\n" + 
		"successor_list: " +  successor_list + "\n" + 
		"finger_table: " + finger_table;
	}

	public void showState() {
		System.out.println( toStringFull() );
	}
}
