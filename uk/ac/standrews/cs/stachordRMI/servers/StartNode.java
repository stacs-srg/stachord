package uk.ac.standrews.cs.stachordRMI.servers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.SortedSet;
import java.util.TreeSet;


import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.util.NodeComparator;
import uk.ac.standrews.cs.stachordRMI.util.RingStabilizer;

/**
 * Provides the entry point for deploying a Chord node that is joining a Chord Ring
 *
 * * Creates a Chord node that joins an existing ring.
 *
 * Two command line parameters are accepted:
 * <dl>
 *	<dt>-k[host][:port]</dt>
 *	<dd>Specifies the hostname/ip address,port for a known host that will be used to join the Chord ring
 * 		Note: if no host is specified i.e. "-k", "-k:" or "-k:12345" then then the local loopback address (127.0.0.1) is used.
 * 		If a host is specified, but no port, then this node will try to contact the remote known node on the default port, 52525. </dd>
 * 	<dt>-s[host][:port]</dt>
 * 	<dd>Specifies the host address and port for the local machine on which the Chord services should be made available.
 * 	</dd>
 * </dl>
 * 
 * @author al
 */

public class StartNode  {
	
	private static final DiagnosticLevel DEFAULT_DIAGNOSTIC_LEVEL = DiagnosticLevel.FULL;

	private static final String RMI_POLICY_FILENAME = "rmiPolicy";
	
	private static final int default_port = 52525;
	
	private static final String default_address = "127.0.0.1";

	public static void main ( String[] args ) {
		//this may be overridden by a CLA
		Diagnostic.setLevel(DEFAULT_DIAGNOSTIC_LEVEL);

		Diagnostic.setTimestampFlag(true);
		Diagnostic.setTimestampFormat(new SimpleDateFormat("HH:mm:ss:SSS "));
		Diagnostic.setTimestampDelimiterFlag(false);
		ErrorHandling.setTimestampFlag(false);

		// RMI Policy runes from Ben 
		
		System.setProperty("java.security.policy", RMI_POLICY_FILENAME); 
		if (System.getSecurityManager() == null) {
			ErrorHandling.error( "Cannot find secrity manager" );
			System.setSecurityManager(new RMISecurityManager());
		}
		
		String server_address_parameter = null;
		String local_address = default_address;
		int local_port = default_port; // default port
		try {
			server_address_parameter = CommandLineArgs.getArg(args, "-s"); // This nodes's address
			if (server_address_parameter != null) {
				local_address = NetworkUtil.extractHostName(server_address_parameter);
				local_port = NetworkUtil.extractPortNumber(server_address_parameter);
			}else {
				ErrorHandling.hardError( "No local address specified - use -s option" );
			}
		} catch( Exception e ) {
			ErrorHandling.hardError( "Exception in establishing local socket addresses" + server_address_parameter );
		}
		String known_address_parameter = null;
		String known_address = null;
		int known_port = default_port;
		try {
			known_address_parameter = CommandLineArgs.getArg(args, "-k"); // Known ChordNode
			if (known_address_parameter != null) {
				known_address = NetworkUtil.extractHostName(known_address_parameter);
				known_port = NetworkUtil.extractPortNumber(known_address_parameter);
			} else {
				known_address_parameter = "unspecified";
			}
		} catch( Exception e ) {
			ErrorHandling.hardError( "Exception in establishing known node socket addresses" + known_address_parameter );
		}
		Diagnostic.traceNoSource(DiagnosticLevel.RUN, "Starting RMI Chord node with local address: " + server_address_parameter + " and known node address: " + known_address_parameter );
		IChordNode node = joinChordRing( local_address, local_port, known_address, known_port );

	}


	/**
	 * Join an existing chord ring.
	 * 	
	 * @param localHostname 	The hostname on which this node will start. This must be a local address to the machine
	 * 	on which this process is running. 
	 * @param localPort			The port on which this node will listen. The RMI server will run on this port.
	 * @param knownHostName	The hostname of a known host in the existing Chord ring.
	 * @param knownPort		The port on which a known host is listening.
	 * @param databaseName		The name of the database instance starting this Chord ring. This information is used purely
	 * 	for diagnostic output, so can be left null.	
	 * @return true if a node was successfully created and joined an existing Chord ring; otherwise false.
	 */
	public static IChordNode joinChordRing( String localHostname, int localPort, String knownHostName, int knownPort ) {

		InetSocketAddress localChordAddress = new InetSocketAddress(localHostname, localPort);
		InetSocketAddress knownHostAddress = new InetSocketAddress(knownHostName, knownPort);

		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Connecting to existing Chord ring on " + knownHostName + ":" + knownPort);

		IChordNode chordNode = null;
		
		try {
			chordNode = ChordNodeImpl.deployNode(localChordAddress, knownHostAddress);
			Diagnostic.trace( "Node running at address: " + localChordAddress.getHostName() + ":" + localChordAddress.getPort() );
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

//		((ChordNodeImpl)chordNode).addObserver(this); <<<<<<<<<<<<<<<

//		for (IChordNode node: allNodes){
//			System.out.println("CHECK. Suc: " + node.getSuccessor());
//		}

		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Started local Chord node on : " + 
				localHostname + " : " + localPort + " : initialized with key :" + chordNode.getKey().toString(10) + 
				" : " + chordNode.getKey() + chordNode.getSuccessor().getKey());


		return chordNode;
	}


}
