package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.SortedSet;

import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeProxy;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.servers.StartNode;
import uk.ac.standrews.cs.stachordRMI.servers.StartRing;

/**
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class InProcessFactory extends AbstractNetworkFactory implements INetworkFactory {

	public INetwork makeNetwork(int number_of_nodes, String network_type) throws IOException, NotBoundException {
		
		initNetwork(number_of_nodes, network_type);
		
		IChordNode first = StartRing.startChordRing(LOCAL_HOST, FIRST_NODE_PORT, node_keys[0]);
		nodes.add(first.getProxy());
		
		for (int port_index = 1; port_index < number_of_nodes; port_index++) {
			
			int port = node_ports[port_index];
			int join_port = node_ports[randomPortIndex(0, port_index)];

			IChordNode next = StartNode.joinChordRing(LOCAL_HOST, port, LOCAL_HOST, join_port, node_keys[port - FIRST_NODE_PORT]);
			nodes.add(next.getProxy());
		}

		// For next time, adjust first node port beyond the ports just used.
		FIRST_NODE_PORT = node_ports[number_of_nodes - 1] + 1;

		return new INetwork() {

			public SortedSet<IChordRemoteReference> getNodes() {
				
				return nodes;
			}

			public void killNode(IChordRemoteReference node) {

				synchronized (nodes) {
					nodes.remove(node);
					((ChordNodeProxy)node.getRemote()).destroy();
				}
			}

			public void killAllNodes() {
				
				synchronized (nodes) {
					for (IChordRemoteReference node : getNodes()) {
						((ChordNodeProxy)node.getRemote()).destroy();
					}
					nodes.clear();
				}
			}			
		};
	}
}
