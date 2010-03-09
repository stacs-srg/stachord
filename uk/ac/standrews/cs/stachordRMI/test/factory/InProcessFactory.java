package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.rmi.RemoteException;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.servers.StartNode;
import uk.ac.standrews.cs.stachordRMI.servers.StartRing;
import uk.ac.standrews.cs.stachordRMI.util.NodeComparator;

/**
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class InProcessFactory implements INodeFactory {

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
		IChordNode first = StartRing.startChordRing( known_node_host, known_node_port );
		allNodes.add( first.getProxy().getRemote() );
		for( int port = known_node_port + 1; port < known_node_port + number_of_nodes; port++ ) {
			IChordNode next = StartNode.joinChordRing( this_host, port, known_node_host, known_node_port );
			allNodes.add( next.getProxy().getRemote() );
		}
				

		return allNodes;
	}
	
	public void deleteNode( IChordRemote node ) {
		// TODO
		//allNodes.remove(cn);
		//cn.destroy();
		
	}


}
