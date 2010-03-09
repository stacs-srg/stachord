package uk.ac.standrews.cs.stachordRMI.servers;

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
 * Provides the entry point for deploying a Chord node that creates a new Chord Ring
 *
 * Creates a Chord node that creates a new ring.
 *
 * A single command line parameter is premitted:
 * <dl>
 * 	<dt>-s[host][:port]</dt>
 * 	<dd>Specifies the host address and port for the local machine on which the Chord services should be made available.
 * 	</dd>
 * </dl>
 * 
 * @author al
 */

public class StartRing  {
		
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
			
			Diagnostic.traceNoSource(DiagnosticLevel.RUN, "Starting new RMI Chord ring with address: " + local_address + " on port: " + local_port );
			try {
				IChordNode node = startChordRing( local_address, local_port );
			} catch (RemoteException e) {
				ErrorHandling.exceptionError(e, "starting new RMI Chord ring" );
			} catch (P2PNodeException e) {
				ErrorHandling.exceptionError(e, "starting new RMI Chord ring" );
			}

		}



	public static IChordNode startChordRing(String hostname, int port) throws RemoteException, P2PNodeException {

		InetSocketAddress localChordAddress = new InetSocketAddress(hostname, port);
		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Deploying new Chord ring on " + hostname + ":" + port);

		IChordNode chordNode = null;
		chordNode  = ChordNodeImpl.deployNode(localChordAddress, null);

		if (chordNode == null){
			ErrorHandling.hardError("Failed to create Chord Node.");
		}

//		((ChordNodeImpl)chordNode).addObserver(this);

		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Started local Chord node on : " + hostname + ":" + port + 
				" : initialized with key :" + chordNode.getKey().toString(10) + " : " + chordNode.getKey()  );

		return chordNode;
	}

}
