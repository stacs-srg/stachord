package uk.ac.standrews.cs.stachordRMI.test;

import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Observable;
import java.util.Observer;
import java.util.SortedSet;
import java.util.TreeSet;


import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.servers.ChordServer;
import uk.ac.standrews.cs.stachordRMI.util.NodeComparator;
import uk.ac.standrews.cs.stachordRMI.util.RingStabilizer;

/**

 * @author Angus Macdonald (angus@cs.st-andrews.ac.uk)
 */
public class TestSetUp implements Observer {


	/**
	 * Reference to the remote chord node which is responsible for ensuring the schema manager
	 * is running. This node is not necessarily the actual location of the schema manager.
	 */
	private IChordRemoteReference currentSMLocation;

	/**
	 * The port on which the local Chord node is running its RMI server. 
	 */
	private int rmiPort;

	/**
	 * Key factory used to create keys for schema manager lookup and to search for specific machines.
	 */
	private static SHA1KeyFactory keyFactory = new SHA1KeyFactory();


	/**
	 * <p>Set of nodes in the system sorted by key order.
	 * 
	 * <p>This set is only maintained if {@link org.h2.engine.Constants#IS_TEST} is true, and won't
	 * work in anything other than a test environment where each node is in the same address space.
	 */
	public SortedSet<IChordNode> allNodes = new TreeSet<IChordNode>(new NodeComparator());


	public TestSetUp () {
		
	}

	/**
	 * Start a new Chord ring at the specified location.
	 * @param hostname	The hostname on which the Chord ring will be started. This must be a local address to the machine
	 * 	on which this process is running.
	 * @param port	The port on which the Chord node will listen. This is port on which the RMI registry will be created.
	 * @param databaseName	The name of the database instance starting this Chord ring. This information is used purely
	 * 	for diagnostic output, so can be left null.
	 * @return	True if the chord ring was started successfully; otherwise false.
	 * @throws P2PNodeException 
	 * @throws RemoteException 
	 */
	public IChordNode startChordRing(String hostname, int port) throws RemoteException, P2PNodeException {

		this.rmiPort = port;

		InetSocketAddress localChordAddress = new InetSocketAddress(hostname, port);
		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Deploying new Chord ring on " + hostname + ":" + port);

		IChordNode chordNode = null;
		chordNode  = ChordNodeImpl.deployNode(localChordAddress, null);

		allNodes.add(chordNode);

		if (chordNode == null){
			ErrorHandling.hardError("Failed to create Chord Node.");
		}

		this.currentSMLocation = chordNode.getProxy();

		((ChordNodeImpl)chordNode).addObserver(this);

		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Started local Chord node on : " + hostname + ":" + port + 
				" : initialized with key :" + chordNode.getKey().toString(10) + " : " + chordNode.getKey() + " : schema manager at " + currentSMLocation + " : ");

		return chordNode;
	}

	/**
	 * Join an existing chord ring.
	 * 	
	 * @param localHostname 	The hostname on which this node will start. This must be a local address to the machine
	 * 	on which this process is running. 
	 * @param localPort			The port on which this node will listen. The RMI server will run on this port.
	 * @param remoteHostname	The hostname of a known host in the existing Chord ring.
	 * @param remotePort		The port on which a known host is listening.
	 * @param databaseName		The name of the database instance starting this Chord ring. This information is used purely
	 * 	for diagnostic output, so can be left null.	
	 * @return true if a node was successfully created and joined an existing Chord ring; otherwise false.
	 */
	public IChordNode joinChordRing(String localHostname, int localPort, String remoteHostname, int remotePort ) {

		this.rmiPort = localPort;

		InetSocketAddress localChordAddress = new InetSocketAddress(localHostname, localPort);
		InetSocketAddress knownHostAddress = new InetSocketAddress(remoteHostname, remotePort);

		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Connecting to existing Chord ring on " + remoteHostname + ":" + remotePort);

		IChordNode chordNode = null;
		
		try {
			chordNode = ChordNodeImpl.deployNode(localChordAddress, knownHostAddress);
			allNodes.add(chordNode);
		} catch (RemoteException e) {
			e.printStackTrace();
			return null;
		} catch (P2PNodeException e) {
			e.printStackTrace();
			return null;
		}	

		if (chordNode == null){
			ErrorHandling.hardError("Failed to create Chord Node.");
			return null;
		}

		((ChordNodeImpl)chordNode).addObserver(this);

//		for (IChordNode node: allNodes){
//			System.out.println("CHECK. Suc: " + node.getSuccessor());
//		}

		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Started local Chord node on : " + 
				localHostname + " : " + localPort + " : initialized with key :" + chordNode.getKey().toString(10) + 
				" : " + chordNode.getKey() + " : schema manager at " + currentSMLocation + " : " + chordNode.getSuccessor().getKey());


		return chordNode;
	}
	
	/*
	 *	Ensure the ring is stable before continuing with any tests. 
	 */
	public void stabiliseRing() {
		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "STABALIZING RING::::::");
		
		RingStabilizer.waitForStableNetwork(allNodes);
	}

	
	public void deleteNode( IChordNode cn ) {
		allNodes.remove(cn);
		cn.destroy();
		
	}


	/**
	 * Called by various chord functions in {@link ChordNodeImpl} which are being observed. Of particular interest
	 * to this class is the case where the predecessor of a node changes. This is used to assess whether the schema managers
	 * location has changed.
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable o, Object arg) {

		if (arg.equals(ChordNodeImpl.PREDECESSOR_CHANGE_EVENT)){

			Diagnostic.traceNoEvent(DiagnosticLevel.FULL, ChordNodeImpl.PREDECESSOR_CHANGE_EVENT);
			
		} else if (arg.equals(ChordNodeImpl.SUCCESSOR_CHANGE_EVENT)){
			
			Diagnostic.traceNoEvent(DiagnosticLevel.FULL, ChordNodeImpl.PREDECESSOR_CHANGE_EVENT);
			
		}
	}


}
