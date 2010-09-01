/*******************************************************************************
 * StAChord Library
 * Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 * http://asa.cs.st-andrews.ac.uk/
 * 
 * This file is part of stachordRMI.
 * 
 * stachordRMI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * stachordRMI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with stachordRMI.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.math.BigInteger;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.ActionQueue;
import uk.ac.standrews.cs.nds.util.ActionWithNoResult;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.remote_management.infrastructure.MachineDescriptor;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.servers.AbstractServer;
import uk.ac.standrews.cs.stachordRMI.servers.StartNode;
import uk.ac.standrews.cs.stachordRMI.servers.StartRing;
import uk.ac.standrews.cs.stachordRMI.util.NodeComparator;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Network comprising Chord nodes running on a set of specified physical machines running Linux or OSX.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class MultipleMachineNetwork implements INetwork {
	
	static final String LOCAL_HOST = "localhost";

	private static int next_port = 54496;                         // The next port to be used; static to allow multiple concurrent networks.
	private static final Object sync = new Object();              // Used for serializing network creation.
	
	private static final int REGISTRY_RETRY_INTERVAL =    2000;   // Retry connecting to remote nodes at 2s intervals.
	private static final int REGISTRY_TIMEOUT_INTERVAL = 20000;   // Give up after 20s.
	
	private static Random random = new Random(45974927);          // Used to randomize the point of joining a ring.
	
	private IKey[] node_keys;                                     // The keys of the nodes.
	private TreeSet<IChordRemoteReference> nodes;                 // The nodes themselves.

	private Map<IChordRemoteReference, Process> process_table;    // Map to keep track of the process handle for each node.

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Needed for subclass SingleMachineNetwork, but shouldn't be generally accessible.
	 */
	protected MultipleMachineNetwork() {
	}
	
	/**
	 * @param node_descriptors a description of the target physical host for each Chord node to be created
	 * @param key_distribution the required key distribution
	 * 
	 * @throws IOException if the process for a node cannot be created
	 * @throws SSH2Exception if communication with a remote host fails
	 * @throws InterruptedException 
	 */
	public MultipleMachineNetwork(final MachineDescriptor[] node_descriptors, KeyDistribution key_distribution) throws IOException, SSH2Exception, InterruptedException {
		
		// Initialisation performed in separate method to allow subclass SingleMachineNetwork to catch SSH exception.
		init(node_descriptors, key_distribution);
	}

	protected void init(final MachineDescriptor[] node_descriptors, KeyDistribution key_distribution) throws IOException, SSH2Exception, InterruptedException {
		
		process_table = new HashMap<IChordRemoteReference, Process>();
			
		node_keys = generateNodeKeys(key_distribution, node_descriptors.length);
		nodes = new TreeSet<IChordRemoteReference>(new NodeComparator());
		
		IChordRemoteReference first = createFirstNode(node_descriptors[0], node_keys[0], process_table);
		nodes.add(first);
		
		ActionQueue actions = new ActionQueue(node_descriptors.length, 10, 5000);
	
		for (int node_index = 1; node_index < node_descriptors.length; node_index++) {
	
			final int index = node_index;
			
			actions.enqueue(new ActionWithNoResult() {
				
				public void performAction() {
					
					IChordRemoteReference known_node = pickRandomElement(nodes);
					IKey key = node_keys[index];
					
					try {
						IChordRemoteReference next = createJoiningNode(node_descriptors[index], known_node, key, process_table);
						nodes.add(next);
					}
					catch (Exception e) {
						ErrorHandling.exceptionError(e);
					}
				}
			});
		}
		
		actions.blockUntilNoUncompletedActions();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public SortedSet<IChordRemoteReference> getNodes() {
		
		return nodes;
	}

	public void killNode(IChordRemoteReference node) {
		
		synchronized (nodes) {
			
			if (nodes.contains(node)) {

				int network_size = nodes.size();
				
				assert process_table.containsKey(node);

				process_table.get(node).destroy();
				process_table.remove(node);

				boolean successfully_removed = nodes.remove(node);
				assert successfully_removed;

				assert nodes.size() == network_size - 1;
			}
		}
	}

	public void killAllNodes() {
		
		synchronized (nodes) {
			
			for (IChordRemoteReference node : nodes) {
				
				assert process_table.containsKey(node);

				process_table.get(node).destroy();
				process_table.remove(node);
			}
			nodes.clear();
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static IChordRemoteReference createFirstNode(final MachineDescriptor machine_descriptor, final IKey key) throws IOException, SSH2Exception {
		
		return createFirstNode(machine_descriptor, key, null);
	}

	public static IChordRemoteReference createJoiningNode(final MachineDescriptor machine_descriptor, final IChordRemoteReference known_node, final IKey key) throws IOException, SSH2Exception {
		
		return createJoiningNode(machine_descriptor, known_node, key, null);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static String getHost(MachineDescriptor node_descriptor) {
		
		if (node_descriptor != null) return node_descriptor.ssh_client_wrapper.getServer().getHostName();
		else                         return LOCAL_HOST;
	}

	protected static Process runProcess(MachineDescriptor node_descriptor, Class<? extends AbstractServer> node_class, List<String> args) throws IOException, SSH2Exception {
		
		if (node_descriptor != null) {
			if (node_descriptor.lib_urls != null) {
				return Processes.runJavaProcess(node_class, args, node_descriptor.ssh_client_wrapper, node_descriptor.java_version, node_descriptor.lib_urls, node_descriptor.wget_path, node_descriptor.lib_install_dir, true);
			}
			else {
				return Processes.runJavaProcess(node_class, args, node_descriptor.ssh_client_wrapper, node_descriptor.java_version, node_descriptor.class_path);
			}
		}
		else {
			return Processes.runJavaProcess(node_class, args);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private synchronized IChordRemoteReference pickRandomElement(SortedSet<IChordRemoteReference> nodes) {
		
		int index = randomIndex(0, nodes.size());
		int count = 0;
		
		// Iterate through set since no way to pick element with specified index.
		for (IChordRemoteReference reference : nodes) {
			if (count++ == index) return reference;
		}
		
		// Shouldn't reach here.
		return null;
	}
	
	private interface ArgGen {
		List<String> getArgs(int local_port);
	}

	private static IChordRemoteReference createFirstNode(final MachineDescriptor machine_descriptor, final IKey key, Map<IChordRemoteReference, Process> process_table) throws IOException, SSH2Exception {
		
		ArgGen arg_gen = new ArgGen() {
			
			public List<String> getArgs(int local_port) {
				
				List<String> args = new ArrayList<String>();
				
				args.add("-s" + NetworkUtil.formatHostAddress(getHost(machine_descriptor), local_port));
				addKeyArg(key, args);
				
				return args;
			}
		};
		
		return createNode(machine_descriptor, arg_gen, StartRing.class, process_table);
	}

	private static IChordRemoteReference createJoiningNode(final MachineDescriptor machine_descriptor, final IChordRemoteReference known_node, final IKey key, Map<IChordRemoteReference, Process> process_table) throws IOException, SSH2Exception {
		
		ArgGen arg_gen = new ArgGen() {
			
			public List<String> getArgs(int local_port) {
				
				List<String> args = new ArrayList<String>();
				
				args.add("-s" + NetworkUtil.formatHostAddress(getHost(machine_descriptor), local_port));
				args.add("-k" + NetworkUtil.formatHostAddress(known_node.getAddress())); 
				addKeyArg(key, args);
				
				return args;
			}
		};
		
		return createNode(machine_descriptor, arg_gen, StartNode.class, process_table);
	}

	// Generates a random index such that lower <= index < upper.
	private int randomIndex(int lower, int upper) {
		
		return lower + random.nextInt(upper - lower);
	}
	
	private IKey[] generateNodeKeys(KeyDistribution network_type, int number_of_nodes) {
		
		IKey[] node_keys = new IKey[number_of_nodes];
		
		// Leave null entries for RANDOM since the keys will be generated from hashing addresses.
		if (network_type != KeyDistribution.RANDOM) {
		
			BigInteger key_value = null;
			BigInteger node_increment = null;
	
			if (network_type == KeyDistribution.EVEN) {
				
				key_value = new BigInteger("0");
				node_increment = Key.KEYSPACE_SIZE.divide(new BigInteger(String.valueOf(number_of_nodes)));	
			}
			
			if (network_type == KeyDistribution.CLUSTERED) {
				
				// Span the wrap-over point just to be awkward.
				key_value = Key.KEYSPACE_SIZE.subtract(new BigInteger(String.valueOf(number_of_nodes / 2)));
				node_increment = new BigInteger("1");
			}
			
			for (int i = 0; i < number_of_nodes; i++) {
				node_keys[i] = new Key(key_value);
				key_value = key_value.add(node_increment);
			}
		}
		
		return node_keys;
	}

	private static void addKeyArg(IKey key, List<String> args) {
	
		if (key != null) args.add("-x" + key.toString(Key.DEFAULT_RADIX)); 
	}

	private static IChordRemoteReference bindToNode(String host, int port) throws TimeoutException {
				
		long start_time = System.currentTimeMillis();
		
		while (true) {
		
			try {
				return ChordNodeImpl.bindToNode(NetworkUtil.getInetSocketAddress(host, port));
			}
			catch (RemoteException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "registry location failed: " + e.getMessage());
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
			
			long duration = System.currentTimeMillis() - start_time;
			if (duration > REGISTRY_TIMEOUT_INTERVAL) throw new TimeoutException();
		}
	}

	private static IChordRemoteReference createNode(MachineDescriptor node_descriptor, ArgGen arg_gen, Class<? extends AbstractServer> node_class, Map<IChordRemoteReference, Process> process_table) throws IOException, SSH2Exception {
		
		boolean finished = false;
		IChordRemoteReference node = null;
		
		while (!finished) {
			
			int port = 0;
			
			synchronized (sync) {
				port = next_port++;
			}
		
			List<String> args = arg_gen.getArgs(port);
			
			Process p = runProcess(node_descriptor, node_class, args);
			
			try {
				node = bindToNode(getHost(node_descriptor), port);
				if (process_table != null) process_table.put(node, p);
				finished = true;
			}
			catch (TimeoutException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "timed out trying to connect to port: " + port);
			}
		}
		
		return node;
	}
}
