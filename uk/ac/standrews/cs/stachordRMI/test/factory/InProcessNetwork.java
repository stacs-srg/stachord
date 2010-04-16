package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeProxy;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.servers.StartNode;
import uk.ac.standrews.cs.stachordRMI.servers.StartRing;

/**
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class InProcessNetwork extends AbstractNetwork {

	public InProcessNetwork(int number_of_nodes, String network_type) throws IOException, NotBoundException {

		super(number_of_nodes, network_type);
		
		IChordRemoteReference first = createFirstNode(node_ports[0], node_keys[0]);
		nodes.add(first);
		
		for (int port_index = 1; port_index < number_of_nodes; port_index++) {
			
			int port =      node_ports[port_index];
			int join_port = node_ports[randomPortIndex(0, port_index)];
			IKey key =      node_keys[port_index];
			
			IChordRemoteReference next = createJoiningNode(port, join_port, key);
			nodes.add(next);
		}

		// For next time, adjust first node port beyond the ports just used.
		FIRST_NODE_PORT = node_ports[number_of_nodes - 1] + 1;
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

	private IChordRemoteReference createFirstNode(int port, IKey key) throws RemoteException, NotBoundException {
		
		return StartRing.startChordRing(LOCAL_HOST, port, key).getProxy();
	}

	private IChordRemoteReference createJoiningNode(int port, int join_port, IKey key) throws RemoteException, NotBoundException {
		
		return StartNode.joinChordRing(LOCAL_HOST, port, LOCAL_HOST, join_port, key).getProxy();
	}
}
