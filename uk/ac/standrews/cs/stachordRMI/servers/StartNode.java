package uk.ac.standrews.cs.stachordRMI.servers;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;

/**
 * Provides the entry point for deploying a Chord node that is joining a Chord Ring
 *
 * * Creates a Chord node that joins an existing ring.
 *
 * Two command line parameters are required:
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
public class StartNode extends AbstractServer {

	public static void main(String[] args) throws RemoteException, NotBoundException {
		
		setup(args);
		
		String known_address_parameter = CommandLineArgs.getArg(args, "-k"); // Known ChordNode
		if (known_address_parameter == null) usage();

		String known_address = NetworkUtil.extractHostName(known_address_parameter);
		int known_port =       NetworkUtil.extractPortNumber(known_address_parameter);

		Diagnostic.traceNoSource(DiagnosticLevel.FULL, "Joining RMI Chord ring with address: ", local_address, " on port: ", local_port, ", known node: ", known_address, " on port: ", known_port, " with key: ", server_key);

		joinChordRing(local_address, local_port, known_address, known_port, server_key);
	}

	/**
	 * Join an existing chord ring.
	 * 	
	 * @param hostname  	The hostname on which this node will start. This must be a local address to the machine on which this process is running. 
	 * @param port			The port on which this node will listen. The RMI server will run on this port.
	 * @param known_address	The hostname of a known host in the existing Chord ring.
	 * @param known_port	The port on which a known host is listening.
	 * @throws P2PNodeException 
	 * @throws RemoteException 
	 * @throws NotBoundException 
	 */
	public static IChordNode joinChordRing(String hostname, int port, String known_address, int known_port) throws RemoteException, NotBoundException {

		return joinChordRing(hostname, port, known_address, known_port, null);
	}

	/**
	 * Join an existing chord ring.
	 * 	
	 * @param hostname  	the hostname on which this node will start - must be a local address to the machine on which this process is running
	 * @param port			the port on which this node will listen - the RMI server will run on this port
	 * @param known_address	the hostname of a known host in the existing Chord ring
	 * @param known_port	the port on which a known host is listening
	 * @param node_key      the desired key for the new node
	 * @throws P2PNodeException 
	 * @throws RemoteException 
	 * @throws NotBoundException 
	 */
	public static IChordNode joinChordRing( String hostname, int port, String known_address, int known_port, IKey node_key) throws RemoteException, NotBoundException {

		System.out.println("joinChordRing: " + hostname + " " + port + " " + known_address + " " + known_port + " " + node_key);
		
		
		InetSocketAddress localChordAddress = new InetSocketAddress(hostname, port);
		InetSocketAddress knownHostAddress =  new InetSocketAddress(known_address, known_port);

		if (node_key == null) return new ChordNodeImpl(localChordAddress, knownHostAddress);
		else                  return new ChordNodeImpl(localChordAddress, knownHostAddress, node_key);
	}

	private static void usage() {
			
		ErrorHandling.hardError( "Usage: -k[host][:port]" );
	}
}
