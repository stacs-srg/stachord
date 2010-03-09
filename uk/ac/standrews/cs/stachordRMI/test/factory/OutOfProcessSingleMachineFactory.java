package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.servers.StartNode;
import uk.ac.standrews.cs.stachordRMI.servers.StartRing;
import uk.ac.standrews.cs.stachordRMI.util.NodeComparator;

/**
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class OutOfProcessSingleMachineFactory implements INodeFactory {

	private int known_node_port = 54446;
	
	private static final String known_node_host = "localhost";
	private static final String this_host = "localhost";

	
	/**
	 * Reference to the remote chord node which is responsible for ensuring the schema manager
	 * is running. This node is not necessarily the actual location of the schema manager.
	 */

	/**
	 * <p>Set of nodes in the system sorted by key order.
	 * 
	 */
	public SortedSet<IChordRemote> allNodes = new TreeSet<IChordRemote>(new NodeComparator());

	/***************** INoideFactory methods  *****************/


	public SortedSet<IChordRemote> makeNetwork( int number_of_nodes ) throws RemoteException, P2PNodeException {
		List<String> args = new ArrayList<String>();
		args.add( known_node_host );
		args.add( Integer.toString( known_node_port ) );
		try {
			Process p = Processes.runJavaProcess( StartRing.class, args );
		} catch (IOException e) {
			ErrorHandling.hardError("Failed to create first Chord Node.");
		}
		try {
			
			
			// *******  THIS IS WIERD TOO - THOUGHT THIS MIGHT BE THE PROBLEM _ IT ISN'T.
			// WE DONT NEED isa below....
			InetSocketAddress isa = new InetSocketAddress( known_node_host, known_node_port );
			
			// ******* Graham - This bit below is wierd - I took it apart to see what the error was..
			// It can be folded back onto one line
			
			// Another test from al to check Security Manager
			
			try {
				System.getSecurityManager().checkConnect( isa.getHostName(), isa.getPort() );
			}
			catch( Exception e ) {
				ErrorHandling.error("Cannot connect to " +  isa.getHostName() + ":" + isa.getPort() );
			}
			
			
			Registry reg = null;
			try {
				reg = LocateRegistry.getRegistry( isa.getHostName(), isa.getPort() ); // isa.getHostName(), isa.getPort() == known_node_host, known_node_port
			} catch ( Exception e1) {
				ErrorHandling.hardError("Cannot find Registry for first deployed Chord Node.");
			}
			if( reg == null ) {
				ErrorHandling.error( "registry is null" );
			}
			IChordRemote first = (IChordRemote) reg.lookup( IChordNode.CHORD_REMOTE_SERVICE );
			// IChordRemote first = (IChordRemote) LocateRegistry.getRegistry( known_node_host, known_node_port ).lookup( IChordNode.CHORD_REMOTE_SERVICE );
			allNodes.add( first );
		} catch (NotBoundException e1) {
			ErrorHandling.hardError("Cannot find first deployed Chord Node.");
		};

		for( int port = known_node_port + 1; port < known_node_port + number_of_nodes; port++ ) {
			args = new ArrayList<String>();
			args.add( this_host );
			args.add( Integer.toString( port ) );
			args.add( known_node_host );
			args.add( Integer.toString( known_node_port ) );
			try {
				Process p = Processes.runJavaProcess( StartNode.class, args );
			} catch (IOException e) {
				ErrorHandling.hardError("Failed to create Chord Node.");
			}
			try {
				IChordRemote next = (IChordRemote) LocateRegistry.getRegistry( this_host, port  ).lookup( IChordNode.CHORD_REMOTE_SERVICE );
				allNodes.add( next );
			} catch (NotBoundException e) {
				ErrorHandling.hardError("Cannot find deployed Chord Node.");
			}

		}


		return allNodes;
	}

	public void deleteNode( IChordRemote node ) {
		// TODO
		//allNodes.remove(cn);
		//cn.destroy();
	}

}



