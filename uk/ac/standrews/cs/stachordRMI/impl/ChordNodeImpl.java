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

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.eventModel.Event;
import uk.ac.standrews.cs.nds.eventModel.IEvent;
import uk.ac.standrews.cs.nds.eventModel.IEventGenerator;
import uk.ac.standrews.cs.nds.eventModel.eventBus.busInterfaces.IEventBus;
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
import uk.ac.standrews.cs.stachordRMI.events.ChordEventFactory;
import uk.ac.standrews.cs.stachordRMI.events.Constants;
import uk.ac.standrews.cs.stachordRMI.fingerTableFactories.GeometricFingerTableFactory;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoPrecedingNodeException;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoReachableNodeException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTable;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTableFactory;
import uk.ac.standrews.cs.stachordRMI.util.SegmentArithmetic;

/**
 * Implementation of Chord node.
 * 
 * @author sja7, stuart, al, graham
 */
public class ChordNodeImpl implements IChordNode, Remote  {

	private InetSocketAddress local_address;
	private String local_address_rep;
	private IKey key;
	private int hash_code;

	private IChordRemote predecessor, successor;
	private SuccessorList successor_list;
	private IFingerTable finger_table;

	private IEventBus bus;
	private IApplicationRegistry registry;
	private IEventGenerator event_generator;
	private boolean simulating_failure;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new Chord node using a specified finger table policy and no event bus.
	 * 
	 * @param local_address the network address of the node
	 * @param key the node's key
	 * @param finger_table_factory a factory for creating finger tables
	 */
	public ChordNodeImpl(InetSocketAddress local_address, IKey key, IFingerTableFactory finger_table_factory) {
		this(local_address, key, null, finger_table_factory, null);
	}

	/**
	 * Creates a new Chord node using a geometric finger table.
	 * 
	 * @param local_address the network address of the node
	 * @param key the node's key
	 * @param bus an event bus
	 * @param registry an application registry
	 */
	public ChordNodeImpl(InetSocketAddress local_address, IKey key, IEventBus bus, IApplicationRegistry registry) {
		this(local_address, key, bus, new GeometricFingerTableFactory(), registry);
	}

	/**
	 * Creates a new Chord node using a geometric finger table. Used by autonomic manager to control event generation.
	 * 
	 * @param local_address the network address of the node
	 * @param key the node's key
	 * @param bus an event bus
	 * @param registry an application registry
	 * @param event_generator an event generator
	 */
	public ChordNodeImpl(InetSocketAddress local_address, IKey key, IEventBus bus, IApplicationRegistry registry, IEventGenerator event_generator) {
		this(local_address, key, bus, new GeometricFingerTableFactory(event_generator), registry, event_generator);
	}

	/**
	 * Creates a new Chord node using a specified finger table policy.
	 * 
	 * @param local_address the network address of the node
	 * @param key the node's key
	 * @param bus an event bus
	 * @param registry an application registry
	 * @param finger_table_factory a factory for creating finger tables
	 */
	public ChordNodeImpl(InetSocketAddress local_address, IKey key, IEventBus bus, IFingerTableFactory finger_table_factory, IApplicationRegistry registry) {
		this(local_address, key, bus, finger_table_factory, registry, null);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Default constructor used in deserialisation.
	 */
	protected ChordNodeImpl() {
		// Deliberately empty.
	}

	private ChordNodeImpl(InetSocketAddress local_address, IKey key, IEventBus bus, IFingerTableFactory fingerTableFactory, IApplicationRegistry registry, IEventGenerator event_generator) {

		this.local_address = local_address;
		local_address_rep = NetworkUtil.formatHostAddress(local_address);
		this.key = key;
		hash_code = hashCode();

		predecessor = null;
		successor = null;
		successor_list = new SuccessorList(this);

		this.bus = bus;
		this.registry = registry;
		this.event_generator = event_generator;

		finger_table = fingerTableFactory.makeFingerTable(this);
		finger_table.setEventGenerator(event_generator);

		simulating_failure = false;

		Diagnostic.trace(DiagnosticLevel.INIT, "initialised with key: ", key);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// INetworkNodeRepresentation

	public InetSocketAddress getAddress() {

		// This method is RAFDA-cached in remote references, so a call to a remote instance should never fail.
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

	public IP2PNode lookup(IKey k) throws P2PNodeException {
		checkSimulatedFailure();

		/* If the key specified is greater than this node's key, and less than or equal
		 * to this node's successor's key, return this node's successor.
		 * Else iteratively call findSuccessor on nodes preceding the given
		 * key, until a suitable successor for the key is found (when the
		 * key is greater than the current node's key, and less than or
		 * equal to the current node's successor's key).
		 */

		long start_time = System.currentTimeMillis();

		// If the key is the same as this node's key, or if this node's successor is itself (one-node ring), then return this node.
		if (k.equals(key) || successor == this) {

			recordEvent(Constants.FIND_SUCCESSOR_TIME_MONITORING_EVENT, start_time);
			return this;
		}

		// If the key lies between this node and its successor, return the successor.
		if (inSuccessorKeyRange(k)) {

			recordEvent(Constants.FIND_SUCCESSOR_TIME_MONITORING_EVENT, start_time);
			return successor;
		}

		return findNonLocalSuccessor(k, start_time);
	}

	public int routingStateSize() {

		return finger_table.size();
	}

	public boolean isSimulatingFailure() {

		return simulating_failure;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IChordRemote

	public void notify(IChordRemote potential_predecessor) {

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

	public List<IChordRemote> getSuccessorList() {

		checkSimulatedFailure();
		return successor_list.getList();
	}

	public List<IChordRemote> getFingerList() {

		checkSimulatedFailure();
		return finger_table.getFingers();
	}

	public IChordRemote getPredecessor() {

		checkSimulatedFailure();
		return predecessor;
	}

	public IChordRemote getSuccessor() {

		checkSimulatedFailure();
		return successor;
	}

	public void isAlive() {

		checkSimulatedFailure();
	}

	public Pair<NextHopResultStatus, IChordRemote> nextHop(IKey k) {

		checkSimulatedFailure();

		// Check whether the key lies in the range between this node and its successor,
		// in which case the successor represents the final hop.

		if (inSuccessorKeyRange(k)) return new Pair<NextHopResultStatus, IChordRemote>(NextHopResultStatus.FINAL, successor);
		else                        return new Pair<NextHopResultStatus, IChordRemote>(NextHopResultStatus.NEXT_HOP, closestPrecedingNode(k));
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
		setSuccessor(this);

		successor_list.refreshList();
	}

	public synchronized void join(IChordRemote known_node) throws P2PNodeException {
		checkSimulatedFailure();
		IChordRemote initial_successor = (IChordRemote)known_node.lookup(key);
		setSuccessor(initial_successor);
	}

	public synchronized void stabilize() {

		checkSimulatedFailure();

		try {
			// Find predecessor of this node's successor.
			IChordRemote predecessor_of_successor = getPredecessorOfSuccessor();

			// Check whether that is a better successor for this node than its current successor.
			// This may update this node's successor pointer.
			boolean found_better_successor = checkForBetterSuccessor(predecessor_of_successor);

			// Notify this node's successor (which may have just changed) that this node may be its predecessor.
			notifySuccessor();

			// Update this node's successor list from its successor's.
			boolean found_changes_in_successor_list = refreshSuccessorList();

			if (!(found_changes_in_successor_list || found_better_successor)) {
				recordEvent(Constants.STABILISE_EVENT, Constants.SELF_REPAIR_HAD_NO_EFFECT);
			}
		}
		catch (Exception e) {

			// Error contacting successor.
			handleSuccessorError(e);
		}
	}

	public synchronized void checkPredecessor() {

		checkSimulatedFailure();

		try {

			pingPredecessor();
			recordEvent(Constants.CHECK_PREDECESSOR_EVENT, Constants.SELF_REPAIR_HAD_NO_EFFECT);
		}
		catch (Exception e) {

			suggestSuspectedFailure(predecessor);
			setPredecessor(null);
			recordEvent(Constants.PREDECCESSOR_ACCESS_EVENT, Constants.FIELD_ACCESS_FAILED);
		}
	}

	public synchronized void fixNextFinger() {
		checkSimulatedFailure();
		finger_table.fixNextFinger();
	}

	public synchronized void setPredecessor(IChordRemote new_predecessor) {
		checkSimulatedFailure();

		predecessor = new_predecessor;
		generateEvent(ChordEventFactory.makePredecessorRepEvent(new_predecessor));
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

	@Override
	public String toString() {

		// This method is RAFDA-cached in remote references, so a call to a remote instance should never fail.
		return local_address_rep;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns this node's event bus.
	 * 
	 * @return the event bus
	 */
	public IEventBus getEventBus() {
		return bus;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected void recordEvent(String event_type, String event_description) {

		if (event_generator != null) {
			event_generator.createAndPublishEvent(event_type, event_description);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void recordEvent(String event_type, long start_time) {

		recordEvent(event_type, String.valueOf(elapsedTime(start_time)));
	}

	/**
	 * Returns the closest preceding node from the finger table, or the successor if there
	 * is no such node in the finger table
	 * 
	 * @param k a key
	 * @return the peer node the closest preceding key to k
	 */
	private IChordRemote closestPrecedingNode(IKey k) {

		try {
			return finger_table.closestPrecedingNode(k);
		}
		catch (NoPrecedingNodeException e) {
			return successor;
		}
	}

	private void pingPredecessor() {

		if (predecessor != null) {
			// Try to communicate with predecessor.
			try {
				predecessor.isAlive();
				recordEvent(Constants.PREDECCESSOR_ACCESS_EVENT, Constants.FIELD_ACCESS_OK);
			} catch (RemoteException e) {
				// do nothing
			}

			
		}
	}

	private IChordRemote getPredecessorOfSuccessor() {

		IChordRemote predecessor_of_successor = null;
		try {
			predecessor_of_successor = successor.getPredecessor();
			recordEvent(Constants.SUCCESSOR_ACCESS_EVENT, Constants.FIELD_ACCESS_OK);
		} catch (RemoteException e) {
			handleSuccessorError( e );
		}
		return predecessor_of_successor;
	}

	private boolean checkForBetterSuccessor(IChordRemote predecessor_of_successor) {

		if (predecessor_of_successor != null) {

			IKey key_of_predecessor_of_successor = predecessor_of_successor.getKey();

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
			successor.notify(this);
			recordEvent(Constants.SUCCESSOR_ACCESS_EVENT, Constants.FIELD_ACCESS_OK);
		} catch (RemoteException e) {
			handleSuccessorError( e );
		}
		
	}

	private boolean refreshSuccessorList() {

		if (successor != this) {

			List<IChordRemote> successor_list_of_successor;
			try {
				successor_list_of_successor = successor.getSuccessorList();
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

	private IChordRemote findNonLocalSuccessor(IKey k, long start_time) throws P2PNodeException {

		// Get the first hop.
		IChordRemote next = closestPrecedingNode(k);
		int hop_count = 0;

		// Used to record hops to generate event on final hop..
		List<IEvent> routing_hops = new ArrayList<IEvent>();

		while (true) {

			Pair<NextHopResultStatus, IChordRemote> result = getNextHop(k, next, hop_count);

			recordEvent(Constants.FINGER_TABLE_ACCESS_EVENT, Constants.FIELD_ACCESS_OK);

			if (bus != null) {
				IEvent e = ChordEventFactory.makeChordRoutingHopEvent(toString(), next.toString());
				routing_hops.add(e);
			}

			switch (result.first) {

				case NEXT_HOP: {

					next = result.second;
					break;
				}

				case FINAL: {

					next = result.second;

					if (bus != null) {
						IEvent e = ChordEventFactory.makeChordRoutingPathEvent(routing_hops, toString(), next.toString());
						bus.publishEvent(e);
					}

					recordEvent(Constants.FIND_SUCCESSOR_TIME_MONITORING_EVENT, start_time);

					return next;
				}

				default: {

					ErrorHandling.hardError("nextHop call returned NextHopResult with unrecognised code");
				}
			}

			hop_count++;
		}
	}

	private Pair<NextHopResultStatus, IChordRemote> getNextHop(IKey k, IChordRemote next, int hop_count) throws P2PNodeException {

		try {
			// Get the next hop.
			return next.nextHop(k);
		}
		catch (Exception e) {

			// Gather events to compute the finger table access error rate.
			recordEvent(Constants.FINGER_TABLE_ACCESS_EVENT, Constants.FIELD_ACCESS_FAILED);

			// If 'next' is this node's successor, record a successor failure.
			// Use locally cached key for comparison, since 'next' appears to have failed.
			if (next.getKey().equals(successor.getKey())) {
				recordEvent(Constants.SUCCESSOR_ACCESS_EVENT, Constants.FIELD_ACCESS_FAILED);
			}

			Diagnostic.trace(DiagnosticLevel.RUN, this, ": signalling suspected failure of ", NetworkUtil.formatHostAddress(next.getAddress()));

			// Only the first hop in the chain is to a finger.
			if (hop_count == 0) {
				suggestSuspectedFingerFailure(next);
			} else {
				suggestSuspectedFailure(next);
			}

			throw new P2PNodeException(P2PStatus.LOOKUP_FAILURE, "a failure ocurred when trying to determine the successor for key " + k + ": " + e.getMessage());
		}
	}

	/**
	 * Sets the successor node in key space.
	 * 
	 * @param successor the new successor node
	 */
	private synchronized void setSuccessor(IChordRemote successor) {
		//ErrorHandling.error(successor.getClass().getName());	
		
//		if(successor.getClass().getName().contains("ChordNodeImpl$IP2PNode$PROXY")){
//			new Exception().printStackTrace(System.err);
//			ErrorHandling.hardError("Successor proxy is the wrong type");
//		}
		
		IChordRemote old_successor = this.successor;
		this.successor = successor;

		generateEvent(ChordEventFactory.makeSuccessorRepEvent(successor.getAddress(), successor.getKey()));
	}

	private boolean inSuccessorKeyRange(IKey k) {

		return SegmentArithmetic.inHalfOpenSegment(k, key, successor.getKey());
	}

	private void checkSimulatedFailure() {

		if (simulating_failure)
			throw new SimulatedFailureException("simulated failure");
	}

	protected void generateEvent(Event e) {

		if (bus != null) {
			bus.publishEvent(e);
		}
	}

	/**
	 * Attempts to find a working successor from the successor list, or failing that using the predecessor or a finger.
	 */
	private synchronized void findWorkingSuccessor() {

		suggestSuspectedFailure(successor);

		try {
			IChordRemote new_successor = successor_list.findFirstWorkingNode();
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

	private void joinUsingFinger() throws P2PNodeException {

		for (IChordRemote node : finger_table.getFingers()) {

			try {
				join(node);
				return;
			}
			catch (P2PNodeException e2) {
				// Ignore and try next finger.
			}
		}

		throw new P2PNodeException(P2PStatus.NODE_JOIN_FAILURE, "couldn't join using any finger");
	}

	/**
	 * Computes the time that has elapsed since the given start time.
	 * 
	 * @param start_time a time stamp as returned by System.currentTimeMillis())
	 * @return the elapsed time
	 */
	private int elapsedTime(long start_time) {

		return (int) ((int) System.currentTimeMillis() - start_time);
	}

	private void suggestSuspectedFailure(IChordRemote node) {

		Diagnostic.trace(DiagnosticLevel.FULL, this,": signalling suspected failure of ", NetworkUtil.formatHostAddress(node.getAddress()));

		generateEvent(ChordEventFactory.makeNodeFailureNotificationRepEvent(node.getAddress(), node.getKey()));
	}

	private void suggestSuspectedFingerFailure(IChordRemote node) {

		// Notify finger table that the node may have failed.
		finger_table.notifySuspectedFailure(node);

		suggestSuspectedFailure(node);
	}
}
