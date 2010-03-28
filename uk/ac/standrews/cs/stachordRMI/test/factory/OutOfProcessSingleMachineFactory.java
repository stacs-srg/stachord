package uk.ac.standrews.cs.stachordRMI.test.factory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.stachordRMI.impl.ChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.servers.StartNode;
import uk.ac.standrews.cs.stachordRMI.servers.StartRing;
import uk.ac.standrews.cs.stachordRMI.util.NodeComparator;

/**
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class OutOfProcessSingleMachineFactory extends AbstractNetworkFactory implements INetworkFactory {

	private static final int REGISTRY_RETRY_INTERVAL = 2000;

	private static int FIRST_NODE_PORT = 54498;
	
	private static final String LOCAL_HOST = "localhost";

	/***************** INodeFactory methods *****************/

	public INetwork makeNetwork(int number_of_nodes, String network_type) throws IOException {
		
		// TODO fix for all network types.
		
		final SortedSet<IChordRemoteReference> nodes = new TreeSet<IChordRemoteReference>(new NodeComparator());
		final Map<IChordRemoteReference, Process> processTable = new HashMap<IChordRemoteReference, Process>();

		List<String> args = new ArrayList<String>();
		args.add( "-s" + LOCAL_HOST + ":" + FIRST_NODE_PORT );
		
		Process firstNodeProcess = Processes.runJavaProcess(StartRing.class, args);
		
		System.out.println("first port: " + FIRST_NODE_PORT);

		IChordRemoteReference first = bindToNode(LOCAL_HOST, FIRST_NODE_PORT);
		nodes.add(first);
		processTable.put(first, firstNodeProcess);

		for (int port = FIRST_NODE_PORT + 1; port < FIRST_NODE_PORT + number_of_nodes; port++) {
			
			int join_port = randomPort(FIRST_NODE_PORT, port);

			args = new ArrayList<String>();

			args.add("-s" + LOCAL_HOST + ":" + port);
			args.add("-k" + LOCAL_HOST + ":" + join_port); 

			Process otherNodeProcess = Processes.runJavaProcess(StartNode.class, args);

			IChordRemoteReference next = bindToNode(LOCAL_HOST, port);
			nodes.add(next);
			processTable.put(next, otherNodeProcess);
		}
		
		// For next time, adjust first node port beyond the ports just used.
		FIRST_NODE_PORT += number_of_nodes;

		return new INetwork() {

			public SortedSet<IChordRemoteReference> getNodes() {
				
				return nodes;
			}

			public void killNode(IChordRemoteReference node) {

				synchronized (nodes) {
					int network_size = nodes.size();
					assertTrue(nodes.contains(node));

					processTable.get(node).destroy();
					
					assertTrue(nodes.contains(node));
					assertTrue(nodes.remove(node));
					assertEquals(nodes.size(), network_size - 1);
				}
			}

			public void killAllNodes() {
				
				synchronized (nodes) {
				
					for (IChordRemoteReference node : getNodes()) {
						processTable.get(node).destroy();
					}
					nodes.clear();
				}
			}
		};
	}

	private IChordRemoteReference bindToNode(String host, int port) {
		
		while (true) {
		
			try {
				Registry reg = LocateRegistry.getRegistry(host, port);
				IChordRemote remote = (IChordRemote) reg.lookup(IChordNode.CHORD_REMOTE_SERVICE);
				return new ChordRemoteReference(remote.getKey(), remote);
			}
			catch (RemoteException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "registry location failed");
			}
			catch (NotBoundException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "binding to node in registry failed");
			}
			catch (Exception e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "registry lookup failed");
			}
			
			try {
				Thread.sleep(REGISTRY_RETRY_INTERVAL);
			}
			catch (InterruptedException e) {
			}
		}
	}
}
