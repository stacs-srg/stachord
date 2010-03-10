package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
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
public class OutOfProcessSingleMachineFactory implements INetworkFactory {

	private static final int REGISTRY_RETRY_INTERVAL = 2000;

	private static int FIRST_NODE_PORT = 54446;
	private static final String LOCAL_HOST = "localhost";

	/***************** INodeFactory methods *****************/

	public INetwork makeNetwork( int number_of_nodes ) throws P2PNodeException, IOException {
		
		final SortedSet<IChordRemote> allNodes = new TreeSet<IChordRemote>(new NodeComparator());
		final Map<IChordRemote, Process> processTable = new HashMap<IChordRemote, Process>();
		
		List<String> args = new ArrayList<String>();
		args.add( "-s" + LOCAL_HOST + ":" + FIRST_NODE_PORT );
		
		Process firstNodeProcess = Processes.runJavaProcess( StartRing.class, args );

		IChordRemote first = bindToNode(LOCAL_HOST, FIRST_NODE_PORT);
		allNodes.add( first );		
		processTable.put(first, firstNodeProcess);

		for( int port = FIRST_NODE_PORT + 1; port < FIRST_NODE_PORT + number_of_nodes; port++ ) {
			
			args = new ArrayList<String>();

			args.add( "-s" + LOCAL_HOST + ":" + port );
			args.add( "-k" + LOCAL_HOST + ":" + FIRST_NODE_PORT );

			Process otherNodeProcess = Processes.runJavaProcess( StartNode.class, args );

			IChordRemote next = bindToNode( LOCAL_HOST, port  );
			allNodes.add( next );
			processTable.put(next, otherNodeProcess);
		}
		
		// For next time, adjust first node port beyond the ports just used.
		FIRST_NODE_PORT += number_of_nodes;

		return new INetwork() {

			public SortedSet<IChordRemote> getNodes() {
				
				return allNodes;
			}

			public void killNode(IChordRemote node) {

				processTable.get(node).destroy();
			}

			public void killAllNodes() {
				
				for (IChordRemote node : getNodes()) {
					killNode(node);
				}
			}			
		};
	}

	private IChordRemote bindToNode(String host, int port) {
		
		IChordRemote node = null;
		
		while (node == null) {
		
			Diagnostic.trace("trying to bind to node");
			
			Registry reg = null;
			try {
				reg = LocateRegistry.getRegistry( host, port );
				node = (IChordRemote) reg.lookup( IChordNode.CHORD_REMOTE_SERVICE );
				break;
			}
			catch (RemoteException e) {
				Diagnostic.trace("registry location failed");
			}
			catch (NotBoundException e) {
				Diagnostic.trace("binding to node in registry failed");
			}
			catch (Exception e) {
				Diagnostic.trace("registry lookup failed");
			}
			
			try {
				Thread.sleep(REGISTRY_RETRY_INTERVAL);
			}
			catch (InterruptedException e) {
			}
		}

		return node;
	}
}
