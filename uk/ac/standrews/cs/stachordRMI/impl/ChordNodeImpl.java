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
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import uk.ac.standrews.cs.nds.eventModel.Event;
import uk.ac.standrews.cs.nds.eventModel.IEvent;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.P2PStatus;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PNode;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.Pair;
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
public class ChordNodeImpl extends Observable implements IChordNode, Remote  {

	private InetSocketAddress local_address;
	private IKey key;
	private int hash_code;

	private IChordRemoteReference predecessor, successor;
	private SuccessorList successor_list;
	private FingerTable finger_table;
	
	private IChordRemoteReference self_reference; 			// a local RMI reference to this node
	private ChordNodeProxy self_proxy;						// The RMI reference actually references this proxy

	private MaintenanceThread maintenanceThread;
	
	public static final String SUCCESSOR_STATE_EVENT_TYPE = "SUCCESSOR_STATE_EVENT";

	public static IEvent PREDECESSOR_CHANGE_EVENT = new Event("PREDECESSOR_CHANGE_EVENT");
	public static IEvent SUCCESSOR_STATE_EVENT =    new Event(SUCCESSOR_STATE_EVENT_TYPE);
	public static IEvent SUCCESSOR_CHANGE_EVENT =   new Event("SUCCESSOR_CHANGE_EVENT");
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a new Chord node using a geometric finger table.
	 * 
	 * @param local_address the network address of the node
	 * @param key the node's key
	 * @param bus an event bus
	 */
	private ChordNodeImpl(InetSocketAddress local_address, IKey key ) {
		
		this.local_address = local_address;
		this.key = key;

		hash_code = hashCode();

		predecessor = null;
		successor = null;
		successor_list = new SuccessorList(this);

		self_proxy = new ChordNodeProxy( this );
		self_reference = new ChordRemoteReference( key,self_proxy  );

		SegmentCalculator src = new SegmentCalculator(this,2.0);
		finger_table = new FingerTable(this,src);

		Diagnostic.trace(DiagnosticLevel.INIT, "initialised with key: ", key);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Default constructor used in deserialisation.
	 */
	protected ChordNodeImpl() {
		// Deliberately empty.
	}
	
	/**
	 * Standard destructor.
	 */
	public void destroy() {
		
		maintenanceThread.stopThread(); // stop the maintenance thread
		try {
			LocateRegistry.getRegistry( local_address.getHostName(), local_address.getPort() ).unbind( IChordNode.CHORD_REMOTE_SERVICE ); // unhook the node from RMI
		}
		catch ( Exception e ) {
			ErrorHandling.error( "Failed to destroy node with key: ", key );
		}
		self_proxy.destroy(); // stop incoming message being processed by this node
		Diagnostic.trace("Successfully destroyed Node with key: ", key);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/*
	 *  Factory method for the creation of ChordNodes.
	 */
	public static IChordNode deployNode(InetSocketAddress local_address, InetSocketAddress known_node_address ) throws RemoteException, P2PNodeException {

		IChordRemoteReference known_node_remote_ref = null;
		
		IKey node_key = new SHA1KeyFactory().generateKey(local_address);
		Diagnostic.trace( DiagnosticLevel.RUN, "Node Key: " + node_key );
		
		// Setup/join the ring
		
		if( known_node_address != null ) {
			try {
				Diagnostic.trace( DiagnosticLevel.RUN, "Lookup RMI Chord node at address: " + known_node_address.getHostName()  + ":" + known_node_address.getPort() );
				IChordRemote known_node_remote = (IChordRemote) LocateRegistry.getRegistry( known_node_address.getHostName(), known_node_address.getPort() ).lookup( IChordNode.CHORD_REMOTE_SERVICE );
				known_node_remote_ref = new ChordRemoteReference( known_node_remote.getKey(), known_node_remote );
			}
			catch (Exception e) {
				throw new RuntimeException( "Serialization error. Path to bad object: ");
			}
		}
		
		ChordNodeImpl node = new ChordNodeImpl( local_address, node_key );

		if (known_node_remote_ref == null) {
			Diagnostic.trace( DiagnosticLevel.RUN, "Creating a new ring" );
			node.createRing();
		}
		else {
			Diagnostic.trace( DiagnosticLevel.RUN, "Joining ring" );
			node.join(known_node_remote_ref);
		}
		
		// Start maintenance thread
		MaintenanceThread thread_for_maintenance = new MaintenanceThread(node);
		thread_for_maintenance.start();
		node.setMaintenanceThread( thread_for_maintenance );
		
		// Now start RMI listening
		try {
			UnicastRemoteObject.exportObject(node.getProxy().getRemote(), 0); // NOTE the remote of the proxy is actually local!
		}
		catch (RemoteException e1) {
			throw new RuntimeException( "Cannot export object ", e1);
		}
		
		// Register the service with the registry
		
		Registry local_registry = null;
		try {
			// Obtains a stub for a registry on the local host on the default registry port
			// first parameter is the port where the RMI registry is listening
			// last parameter is the address where the service is going to be found.
			local_registry = LocateRegistry.createRegistry( local_address.getPort(), null, new CustomSocketFactory( local_address.getAddress() ) );
			Diagnostic.trace( DiagnosticLevel.RUN, "Local Registry deployed at:" + local_address.getAddress() + ":" + local_address.getPort() );
		}
		catch (Exception e) {
			throw new P2PNodeException(P2PStatus.SERVICE_DEPLOYMENT_FAILURE, "could not deploy \"" + IChordNode.CHORD_REMOTE_SERVICE + "\" interface due to registry failure");
		}
		
		try {
			local_registry.rebind( IChordNode.CHORD_REMOTE_SERVICE, node.getProxy().getRemote() );
			Diagnostic.trace( DiagnosticLevel.RUN, "Deployed RMI Chord node in local Registry [" + node + "]" );
		}
		catch (Exception e) {
			throw new P2PNodeException(P2PStatus.SERVICE_DEPLOYMENT_FAILURE, "could not deploy \"" + IChordNode.CHORD_REMOTE_SERVICE + "\" interface due to registry binding exception");
		}
		
		return node;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// INetworkNodeRepresentation

	public InetSocketAddress getAddress() {

		return local_address;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IP2PNodeRepresentation

	public IKey getKey() {

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
		
		/* If the key specified is greater than this node's key, and less than or equal
		 * to this node's successor's key, return this node's successor.
		 * Else iteratively call findSuccessor on nodes preceding the given
		 * key, until a suitable successor for the key is found (when the
		 * key is greater than the current node's key, and less than or
		 * equal to the current node's successor's key).
		 */
		
		// If the key lies between this node and its successor, return the successor.
		if (k.equals(key) || successor.getKey().equals(getKey() ) ) return self_reference;
		else if (inSuccessorKeyRange(k))                            return successor;
		else                                                        return findNonLocalSuccessor(k);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IChordRemote

	public void notify(IChordRemoteReference potential_predecessor) {

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

	public Pair<NextHopResultStatus, IChordRemoteReference> nextHop(IKey k) {

		// Check whether the key lies in the range between this node and its successor,
		// in which case the successor represents the final hop.

		if (inSuccessorKeyRange(k)) return new Pair<NextHopResultStatus, IChordRemoteReference>(NextHopResultStatus.FINAL, successor);
		else                        return new Pair<NextHopResultStatus, IChordRemoteReference>(NextHopResultStatus.NEXT_HOP, closestPrecedingNode(k));
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	// IChordNode

	public synchronized void createRing() {

		setPredecessor(null);
		setSuccessor(self_reference);

		successor_list.refreshList();
	}

	public synchronized void join(IChordRemoteReference known_node) throws RemoteException { 
		
		IChordRemoteReference initial_successor;
		initial_successor = known_node.getRemote().lookup(key);
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
	public synchronized void stabilize() {

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
		catch (Exception e) {
			// Error contacting successor.
			handleSuccessorError(e);
		}
	}

	public synchronized void checkPredecessor() {

		try {
			pingPredecessor();
		}
		catch (Exception e) {
			setPredecessor(null);
		}
	}

	public synchronized void fixNextFinger() {
		
		finger_table.fixNextFinger();
	}

	public synchronized void setPredecessor(IChordRemoteReference new_predecessor) {

		IChordRemoteReference old_predecessor = predecessor;

		predecessor = new_predecessor;

		if (new_predecessor != null && !new_predecessor.equals(old_predecessor)){
			setChanged();
			notifyObservers(PREDECESSOR_CHANGE_EVENT);
		}
	}

	public FingerTable getFingerTable() {

		return finger_table;
	}

	public boolean inLocalKeyRange(IKey k) throws P2PNodeException {

		// Need the predecessor key to test whether k lies within range.
		if (predecessor == null) {
			throw new P2PNodeException(P2PStatus.STATE_ACCESS_FAILURE, "predecessor is null");
		}
		return SegmentArithmetic.inHalfOpenSegment(k, predecessor.getKey(), key);
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

	private IChordRemoteReference getPredecessorOfSuccessor() {

		IChordRemoteReference predecessor_of_successor = null;
		try {
			predecessor_of_successor = successor.getRemote().getPredecessor();
		} catch (RemoteException e) {
			handleSuccessorError( e );
		}
		return predecessor_of_successor;
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

	private void notifySuccessor() {

		try {
			successor.getRemote().notify(this.self_reference);
		} catch (RemoteException e) {
			handleSuccessorError( e );
		}

	}

	private void refreshSuccessorList() {

		if ( !successor.getKey().equals( getKey() ) ) {

			List<IChordRemoteReference> successor_list_of_successor;
			try {
				successor_list_of_successor = successor.getRemote().getSuccessorList();
				successor_list.refreshList(successor_list_of_successor);
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

	private IChordRemoteReference findNonLocalSuccessor(IKey k) throws RemoteException {

		// Get the first hop.
		IChordRemoteReference next = closestPrecedingNode(k);
		int hop_count = 0;

		while (true) {

			Pair<NextHopResultStatus, IChordRemoteReference> result;

			try {
				// Get the next hop.
				result = next.getRemote().nextHop(k);
			}
			catch (RemoteException e) {

				// Only the first hop in the chain is finger on this node.
				if (hop_count == 0) {
					
					Diagnostic.trace(DiagnosticLevel.RUN, this, ": signalling suspected failure of ", next.getKey());
					suggestSuspectedFingerFailure(next);
				}

				throw e;
			}
			
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

	private void setMaintenanceThread(MaintenanceThread maintenanceThread ) {
		
		this.maintenanceThread = maintenanceThread;
	}
	
	/**
	 * Sets the successor node in key space.
	 * 
	 * @param successor the new successor node
	 */
	private synchronized void setSuccessor(IChordRemoteReference successor) {	
		
		IChordRemoteReference oldSuccessor = this.successor;
		this.successor = successor;		
		
		setChanged();

		if (oldSuccessor != null && !oldSuccessor.equals(successor)) {
			notifyObservers(SUCCESSOR_CHANGE_EVENT);
		}
		else {
			notifyObservers( SUCCESSOR_STATE_EVENT );
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
			catch (Exception e1) {   // RemoteException if predecessor has failed, or NullPointerException if it's already null

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

		for (IChordRemoteReference node : finger_table.getFingers()) {

			try {
				join(node);
				return;
			}
			catch (RemoteException e2) {
				// Ignore and try next finger.
			}
		}

		throw new NoReachableNodeException();
	}

	private void suggestSuspectedFingerFailure(IChordRemoteReference node) {

		// Notify finger table that the node may have failed.
		finger_table.notifySuspectedFailure(node);
	}

	public IChordRemoteReference getProxy() {
		return self_reference;
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
			
			try {
				while (running) {
					try {
						sleep(DEFAULT_WAIT_PERIOD);
					}
					catch (InterruptedException e) {}
	
					node.checkPredecessor();
					node.stabilize();
					node.fixNextFinger();
				}
				Diagnostic.trace(DiagnosticLevel.FULL, "maintenance thread stopping on node " + node.getKey());
			}
			catch (OutOfMemoryError e) {
				ErrorHandling.exceptionError(e, "maintenance thread out of memory" );
			}
		}
}

