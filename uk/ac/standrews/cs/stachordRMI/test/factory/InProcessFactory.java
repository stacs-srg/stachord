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
public class InProcessFactory extends AbstractNetworkFactory implements INetworkFactory {

	/***************** INodeFactory methods  *****************/
	
	public INetwork makeNetwork( int number_of_nodes ) throws RemoteException, P2PNodeException {
		
		final SortedSet<IChordRemote> allNodes = new TreeSet<IChordRemote>(new NodeComparator());

		IChordNode first = StartRing.startChordRing( LOCAL_HOST, FIRST_NODE_PORT );
		allNodes.add( first.getProxy().getRemote() );
		
		for( int port = FIRST_NODE_PORT + 1; port < FIRST_NODE_PORT + number_of_nodes; port++ ) {
			
			int join_port = randomPort(FIRST_NODE_PORT, port);
			IChordNode next = StartNode.joinChordRing(LOCAL_HOST, port, LOCAL_HOST, join_port);
			allNodes.add( next.getProxy().getRemote() );
		}
				
		// For next time, adjust first node port beyond the ports just used.
		FIRST_NODE_PORT += number_of_nodes;

		return new INetwork() {

			public SortedSet<IChordRemote> getNodes() {
				
				return allNodes;
			}

			public void killNode(IChordRemote node) {

				// TODO
				//allNodes.remove(cn);
				//cn.destroy();
			}

			public void killAllNodes() {
				
				for (IChordRemote node : getNodes()) {
					killNode(node);
				}
			}			
		};
	}
}
