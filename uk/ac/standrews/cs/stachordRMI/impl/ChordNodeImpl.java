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
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import uk.ac.standrews.cs.nds.eventModel.Event;
import uk.ac.standrews.cs.nds.eventModel.IEvent;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PNode;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.factories.CustomSocketFactory;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoPrecedingNodeException;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoReachableNodeException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.util.SegmentArithmetic;

/**
 * Implementation of Chord node.
 * 
 * @author sja7, stuart, al, graham
 */
public class ChordNodeImpl extends Observable implements IChordNode, IChordRemote, Remote, Observer  {

	private InetSocketAddress local_address;
	private IKey key;
	private int hash_code;

	private IChordRemoteReference predecessor;
	private IChordRemoteReference successor;
	private SuccessorList successor_list;
	private FingerTable finger_table;
	
	private IChordRemoteReference self_reference; 			// A local RMI reference to this node.
	private ChordNodeProxy self_proxy;						// The RMI reference actually references this proxy.

	private MaintenanceThread maintenance_thread;
	private boolean finger_table_maintenance_enabled = true;
	
	public static final String PREDECESSOR_CHANGE_EVENT_TYPE =    "PREDECESSOR_CHANGE_EVENT";
	public static final String SUCCESSOR_STATE_EVENT_TYPE =       "SUCCESSOR_STATE_EVENT";
	public static final String SUCCESSOR_CHANGE_EVENT_TYPE =      "SUCCESSOR_CHANGE_EVENT";
	public static final String SUCCESSOR_LIST_CHANGE_EVENT_TYPE = "SUCCESSOR_LIST_CHANGE_EVENT";
	public static final String FINGER_TABLE_CHANGE_EVENT_TYPE =   "FINGER_TABLE_CHANGE_EVENT";

	public static IEvent PREDECESSOR_CHANGE_EVENT =    new Event(PREDECESSOR_CHANGE_EVENT_TYPE);
	public static IEvent SUCCESSOR_STATE_EVENT =       new Event(SUCCESSOR_STATE_EVENT_TYPE);
	public static IEvent SUCCESSOR_CHANGE_EVENT =      new Event(SUCCESSOR_CHANGE_EVENT_TYPE);
	public static IEvent SUCCESSOR_LIST_CHANGE_EVENT = new Event(SUCCESSOR_LIST_CHANGE_EVENT_TYPE);
	public static IEvent FINGER_TABLE_CHANGE_EVENT =   new Event(FINGER_TABLE_CHANGE_EVENT_TYPE);
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ChordNodeImpl(InetSocketAddress local_address, InetSocketAddress known_node_address) throws RemoteException, NotBoundException {

		this(local_address, known_node_address, new SHA1KeyFactory().generateKey(local_address));
	}

	public ChordNodeImpl(InetSocketAddress local_address, InetSocketAddress known_node_address, IKey key) throws RemoteException, NotBoundException {
		
		this(local_address, known_node_address, key, null);
	}
	
	public ChordNodeImpl(InetSocketAddress local_address, InetSocketAddress known_node_address, IKey key, DiagnosticLevel diagnosticLevel) throws RemoteException, NotBoundException {
		
		this.local_address = local_address;
		this.key = key;

		hash_code = hashCode();

		predecessor = null;
		successor = null;
		
		successor_list = new SuccessorList(this);
		finger_table = new FingerTable(this);

		self_proxy = new ChordNodeProxy(this);
		self_reference = new ChordRemoteReference(key, self_proxy);

		// Setup/join the ring
		
		if (known_node_address == null) {
			createRing();
		}
		else {
			Registry registry = LocateRegistry.getRegistry(known_node_address.getHostName(), known_node_address.getPort());
			IChordRemote known_node_remote = (IChordRemote) registry.lookup(IChordNode.CHORD_REMOTE_SERVICE);
			IChordRemoteReference known_node_remote_ref = new ChordRemoteReference(known_node_remote.getKey(), known_node_remote);
			join(known_node_remote_ref);
		}
		
		// Now start RMI listening.
		UnicastRemoteObject.exportObject(getProxy().getRemote(), 0); // NOTE the remote of the proxy is actually local!

		// Register the service with the registry.
		Registry local_registry = LocateRegistry.createRegistry( local_address.getPort(), null, new CustomSocketFactory( local_address.getAddress() ) );
		local_registry.rebind( IChordNode.CHORD_REMOTE_SERVICE, getProxy().getRemote() );
		
		addObserver(this);

		maintenance_thread = new MaintenanceThread(this);
		maintenance_thread.start();
		
		if (diagnosticLevel == null)
			Diagnostic.setLevel(diagnosticLevel);		
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Standard destructor.
	 */
	public void destroy() {
		
		maintenance_thread.stopThread(); // Stop the maintenance thread.
		try {
			LocateRegistry.getRegistry( local_address.getHostName(), local_address.getPort() ).unbind( IChordNode.CHORD_REMOTE_SERVICE ); // unhook the node from RMI
		}
		catch ( Exception e ) {
			ErrorHandling.error( "Failed to destroy node with key: ", key );
		}
		self_proxy.destroy();            // Stop incoming message being processed by this node.
		Diagnostic.trace("Successfully destroyed Node with key: ", key);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	public InetSocketAddress getAddress() {

		return local_address;
	}

	public IKey getKey() {

		return key;
	}

	public int compareTo(IP2PNode other) {

		if (other == null) return 1;

		return key.compareTo(other.getKey());
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	public IChordRemoteReference lookup(IKey k) throws RemoteException {
		
		/* If the key specified is greater than this node's key, and less than or equal
		 * to this node's successor's key, return this node's successor.
		 * Else iteratively call findSuccessor on nodes preceding the given
		 * key, until a suitable successor for the key is found (when the
		 * key is greater than the current node's key, and less than or
		 * equal to the current node's successor's key).
		 */
		
		if (k.equals(key) || successor.getKey().equals(key) ) {
			
			// If the key is equal to this node's, or the ring currently only has one node...
			return self_reference;
		}
		else {		
			return findNonLocalSuccessor(k);
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IChordRemote

	public void notify(IChordRemoteReference potential_predecessor) {
		
		// Case: predecessor is null and potential_predecessor is this node.
		// We have a one-node ring, and the predecessor should stay null.

		// Case: predecessor is null and potential_predecessor is not this node.
		// Ring has at least two nodes, so any predecessor is better than nothing.

		// Case: predecessor is not null and potential_predecessor is this node.
		// Ignore, since a node's predecessor is never itself.

		// Case: predecessor is not null and potential_predecessor is not in this node's current key range.
		// Ignore, since the current predecessor doesn't appear to have failed, so only valid case is a
		// new node joining.

		// Case: predecessor is not null and potential_predecessor is in this node's current key range.
		// A new node has joined between the current predecessor and this node.

		if (!potential_predecessor.getKey().equals(getKey()) && (predecessor == null || inLocalKeyRange(potential_predecessor.getKey()))) {
			 setPredecessor(potential_predecessor);
		}
	}

	public ArrayList<IChordRemoteReference> getSuccessorList() {

		return successor_list.getList();
	}

	public ArrayList<IChordRemoteReference> getFingerList() {
		
		return finger_table.getFingers();
	}

	public IChordRemoteReference getPredecessor() {

		return predecessor;
	}

	public IChordRemoteReference getSuccessor() {
		
		return successor;
	}

	public void isAlive() {
	}

	public NextHopResult nextHop(IKey k) {

		// Check whether the key lies in the range between this node and its successor,
		// in which case the successor represents the final hop.

		if (inSuccessorKeyRange(k)) return new NextHopResult(true, successor);
		else                        return new NextHopResult(false, closestPrecedingNode(k));
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IChordNode

	public synchronized void createRing() {

		setPredecessor(null);
		setSuccessor(self_reference);

		successor_list.refreshList();
		
		setChanged();
		notifyObservers(SUCCESSOR_LIST_CHANGE_EVENT);
	}

	public synchronized void join(IChordRemoteReference known_node) throws RemoteException { 
		
		IChordRemote remote = known_node.getRemote();
		IChordRemoteReference initial_successor = remote.lookup(key);
		setSuccessor(initial_successor);
	}

	/**
	 * Executes the stabilization protocol.
	 * If the predecessor of this node's current successor is not this node, a new node has joined between this node and
	 * this node's successor. If the new node in between has a key that is
	 * between this node's key, and its successor's key, this node will set its
	 * successor to be the new node and will call the new node's notify method
	 * to tell the new node that it is its predecessor. If the new node is not
	 * in between, this node will call notify on the existing successor telling
	 * it to set its predecessor back to this node.
	 */
	protected synchronized void stabilize() {

		try {
			// Find predecessor of this node's successor.
			IChordRemoteReference predecessor_of_successor = getPredecessorOfSuccessor();

			// Check whether that is a better successor for this node than its current successor.
			// This may update this node's successor pointer.
			checkForBetterSuccessor(predecessor_of_successor);

			// Notify this node's successor (which may have just changed) that this node may be its predecessor.
			notifySuccessor();

			// Update this node's successor list from its successor's.
			refreshSuccessorList();
		}
		catch (RemoteException e) {
			
			// Error contacting successor.
			handleSuccessorError(e);
		}
	}

	public synchronized void checkPredecessor() {

		try {
			pingPredecessor();
		}
		catch (RemoteException e) {
			setPredecessor(null);
		}
	}

	public synchronized void fixNextFinger() {
		
		if (finger_table.fixNextFinger()) {
			
			setChanged();
			notifyObservers(FINGER_TABLE_CHANGE_EVENT);
		}
	}

	public synchronized void setPredecessor(IChordRemoteReference new_predecessor) {

		IChordRemoteReference old_predecessor = predecessor;

		predecessor = new_predecessor;

		if (new_predecessor == null || !new_predecessor.equals(old_predecessor)) {
			
			setChanged();
			notifyObservers(PREDECESSOR_CHANGE_EVENT);
		}
	}

	public FingerTable getFingerTable() {

		return finger_table;
	}

	public boolean inLocalKeyRange(IKey k) {

		// This is never called when the predecessor is null.
		return SegmentArithmetic.inHalfOpenSegment(k, predecessor.getKey(), getKey());
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public int hashCode() {

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

	private IChordRemoteReference getPredecessorOfSuccessor() throws RemoteException {
		
		return successor.getRemote().getPredecessor();
	}

	private void checkForBetterSuccessor(IChordRemoteReference predecessor_of_successor) {

		if (predecessor_of_successor != null) {

			IKey key_of_predecessor_of_successor;
			key_of_predecessor_of_successor = predecessor_of_successor.getKey();

			if (inSuccessorKeyRange(key_of_predecessor_of_successor) && !key_of_predecessor_of_successor.equals(successor.getKey())) {

				// The successor's predecessor is more suitable as this node's successor.
				setSuccessor(predecessor_of_successor);
			}
		}
	}

	private void notifySuccessor() throws RemoteException {

		successor.getRemote().notify(self_reference);
	}

	private void refreshSuccessorList() {

		if (!successor.getKey().equals(getKey())) {

			try {
				List<IChordRemoteReference> successor_list_of_successor = successor.getRemote().getSuccessorList();
				
				if (successor_list.refreshList(successor_list_of_successor)) {

					setChanged();
					notifyObservers(SUCCESSOR_LIST_CHANGE_EVENT);
				}
			}
			catch (RemoteException e) {
				handleSuccessorError( e );
			}
		}
	}

	private void handleSuccessorError(Exception e) {
		
		Diagnostic.trace(DiagnosticLevel.FULL, this, ": error calling successor ", successor, ": ", e);
		findWorkingSuccessor();
	}

	private IChordRemoteReference findNonLocalSuccessor(IKey key) throws RemoteException {

		// Keep track of the hop before the next one, in case the next one turns out to have failed
		// and the one before it has to be notified so it can update its finger table.
		IChordRemote current_hop = this;
		
		// Get the first hop.
		NextHopResult result = nextHop(key);

		while (!result.hopIsFinal()) {

			try {
				// Get the next hop.
				result = result.getHop().getRemote().nextHop(key);
				
				// Remember this hop.
				current_hop = result.getHop().getRemote();
			}
			catch (RemoteException e) {

				// This finger appears to have failed.
				// Tell the node whose finger table contains the finger about the failure.
				current_hop.fingerFailure(result.getHop());
				throw e;
			}
		}
		
		return result.getHop();
	}
	
	public void fingerFailure(IChordRemoteReference broken_finger) {
		
		finger_table.fingerFailure(broken_finger);
	}

	/**
	 * Sets the successor node in key space.
	 * 
	 * @param successor the new successor node
	 */
	private synchronized void setSuccessor(IChordRemoteReference successor) {
		
		if (successor == null) ErrorHandling.hardError("setting successor to null\ncurrent state: " + toStringFull());
		
		IChordRemoteReference oldSuccessor = this.successor;
		this.successor = successor;		
		
		setChanged();

		if (oldSuccessor != null && !oldSuccessor.equals(successor)) {
			notifyObservers(SUCCESSOR_CHANGE_EVENT);
		}
		else {
			notifyObservers(SUCCESSOR_STATE_EVENT);
		}
	}

	private boolean inSuccessorKeyRange(IKey k) {

		return SegmentArithmetic.inHalfOpenSegment(k, key, successor.getKey());
	}

	/**
	 * Attempts to find a working successor from the successor list, or failing that using the predecessor or a finger.
	 */
	private synchronized void findWorkingSuccessor() {

		try {
			IChordRemoteReference new_successor = successor_list.findFirstWorkingNode();
			setSuccessor(new_successor);
		}
		catch (NoReachableNodeException e) {

			try {
				join(predecessor);
			}
			catch (Exception e1) {
				
				// RemoteException if predecessor has failed, or NullPointerException if it's already null

				try {
					joinUsingFinger();
				}
				catch (NoReachableNodeException e2) {

					// Couldn't contact any known node in current ring, so admit defeat and partition ring.
					createRing();
				}
			}
		}
	}
	
	private void joinUsingFinger() throws NoReachableNodeException {

		for (IChordRemoteReference finger : finger_table.getFingers()) {

			if (finger != null && !finger.getKey().equals(getKey()))
			try {
				join(finger);
				return;
			}
			catch (RemoteException e) {
				// Ignore and try next finger.
			}
		}

		throw new NoReachableNodeException();
	}

	public IChordRemoteReference getProxy() {
		return self_reference;
	}

	@Override
	public String toString() {
		return "key: " + key + " local_address: " + local_address;
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

	public void update(Observable o, Object arg) {
		
		String event_type = ((Event)arg).getType();
		Diagnostic.trace(DiagnosticLevel.FULL, ">>>>>>>>>>>>>>>>>>>>>>update: " + event_type);
		
		if (event_type.equals(SUCCESSOR_STATE_EVENT_TYPE)) {
			
			Diagnostic.trace(DiagnosticLevel.FULL, "successor now: ", (successor != null ? successor.getKey() : "null"));
		}
		
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

	public void enableFingerTableMaintenance(boolean enabled) {
		finger_table_maintenance_enabled  = enabled;
	}

	public boolean fingerTableMaintenanceEnabled() {
		return finger_table_maintenance_enabled;
	}
}

class MaintenanceThread extends Thread {
	
	public static final int DEFAULT_WAIT_PERIOD = 1000;
	protected final ChordNodeImpl node;
	protected boolean running = true;
	
	public MaintenanceThread(ChordNodeImpl node){
		this.node = node;
	}
	
	public void stopThread() {
		running = false;
		interrupt();
	}

	@Override
	public void run() {

		while (running) {
			try {
				sleep(DEFAULT_WAIT_PERIOD);
			}
			catch (InterruptedException e) {}
			
			// TODO add toggles for other operations.

			node.checkPredecessor();
			node.stabilize();
			if (node.fingerTableMaintenanceEnabled()) node.fixNextFinger();
		}
		Diagnostic.trace(DiagnosticLevel.FULL, "maintenance thread stopping on node " + node.getKey());
	}
}
