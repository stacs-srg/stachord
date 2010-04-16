package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.rmi.NotBoundException;

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
		
		setupNetwork(number_of_nodes);
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

	protected IChordRemoteReference createFirstNode(int port, IKey key) throws IOException, NotBoundException {
		
		return StartRing.startChordRing(LOCAL_HOST, port, key).getProxy();
	}

	protected IChordRemoteReference createJoiningNode(int port, int join_port, IKey key) throws IOException, NotBoundException {
		
		return StartNode.joinChordRing(LOCAL_HOST, port, LOCAL_HOST, join_port, key).getProxy();
	}
}
