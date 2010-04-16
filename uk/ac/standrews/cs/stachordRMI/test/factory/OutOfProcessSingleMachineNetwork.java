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

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.stachordRMI.impl.ChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.servers.StartNode;
import uk.ac.standrews.cs.stachordRMI.servers.StartRing;

/**
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class OutOfProcessSingleMachineNetwork extends AbstractNetwork {

	private static final int REGISTRY_RETRY_INTERVAL = 2000;
	private static final String LOCAL_HOST = "localhost";

	final Map<IChordRemoteReference, Process> process_table;

	public OutOfProcessSingleMachineNetwork(int number_of_nodes, String network_type) throws IOException {
		
		super(number_of_nodes, network_type);
		
		process_table = new HashMap<IChordRemoteReference, Process>();

		IChordRemoteReference first = createFirstNode(node_ports[0], node_keys[0], process_table);
		nodes.add(first);

		for (int port_index = 1; port_index < number_of_nodes; port_index++) {
			
			int port = node_ports[port_index];
			int join_port = node_ports[randomPortIndex(0, port_index)];

			List<String> args = new ArrayList<String>();

			args.add("-s" + LOCAL_HOST + ":" + port);
			args.add("-k" + LOCAL_HOST + ":" + join_port); 
			addKeyArg(node_keys[port_index], args);

			Process otherNodeProcess = Processes.runJavaProcess(StartNode.class, args);

			IChordRemoteReference next = bindToNode(LOCAL_HOST, port);
			nodes.add(next);
			process_table.put(next, otherNodeProcess);
		}
		
		// For next time, adjust first node port beyond the ports just used.
		FIRST_NODE_PORT = node_ports[number_of_nodes - 1] + 1;
	}

	public void killNode(IChordRemoteReference node) {

		synchronized (nodes) {
			
			int network_size = nodes.size();
			assertTrue(nodes.contains(node));

			process_table.get(node).destroy();
			
			assertTrue(nodes.contains(node));
			assertTrue(nodes.remove(node));
			assertEquals(nodes.size(), network_size - 1);
		}
	}

	public void killAllNodes() {
		
		synchronized (nodes) {
			for (IChordRemoteReference node : getNodes()) {
				process_table.get(node).destroy();
			}
			nodes.clear();
		}
	}

	private IChordRemoteReference createFirstNode(int port, IKey key, Map<IChordRemoteReference, Process> processTable) throws IOException {
		
		List<String> args = new ArrayList<String>();
		args.add("-s" + LOCAL_HOST + ":" + port);
		addKeyArg(key, args);

		Process firstNodeProcess = Processes.runJavaProcess(StartRing.class, args);

		IChordRemoteReference first = bindToNode(LOCAL_HOST, port);
		processTable.put(first, firstNodeProcess);
		return first;
	}

	private void addKeyArg(IKey key, List<String> args) {

		if (key != null) args.add("-x" + key.toString(Key.DEFAULT_RADIX)); 
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
