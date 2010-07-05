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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.stachordRMI.impl.ChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.servers.AbstractServer;
import uk.ac.standrews.cs.stachordRMI.servers.StartNode;
import uk.ac.standrews.cs.stachordRMI.servers.StartRing;
import uk.ac.standrews.cs.stachordRMI.util.NodeComparator;

import com.mindbright.ssh2.SSH2Exception;

public class MultipleMachineNetwork implements INetwork {
	
	protected interface ArgGen {
			List<String> getArgs(int local_port) throws RemoteException;
		}

	public static final String RANDOM =    "RANDOM";       // Nodes randomly distributed around the ring.
	public static final String EVEN =      "EVEN";         // Nodes evenly distributed around the ring.
	public static final String CLUSTERED = "CLUSTERED";    // Nodes clustered tightly in one region of the ring.

	static int next_port = 54496;
	static final Object sync = new Object();
	private static final int REGISTRY_RETRY_INTERVAL = 2000;
	private static final int REGISTRY_TIMEOUT_INTERVAL = 20000;
	
	private static Random random = new Random(45974927);
	
	IKey[] node_keys;
	SortedSet<IChordRemoteReference> nodes;

	protected Map<IChordRemoteReference, Process> process_table;

	protected MultipleMachineNetwork() {
	}

	public MultipleMachineNetwork(NodeDescriptor[] node_descriptors, String network_type) throws IOException, NotBoundException, SSH2Exception {
		
		init(node_descriptors, network_type);
	}

	protected void init(NodeDescriptor[] node_descriptors, String network_type) throws IOException, NotBoundException, SSH2Exception {
		
		process_table = new HashMap<IChordRemoteReference, Process>();
		
		// Serialize creations of networks, since they update static field 'next_port'.
		synchronized (sync) {
			
			if (!network_type.equals(RANDOM) && !network_type.equals(EVEN) && !network_type.equals(CLUSTERED)) fail("unknown network type");
	
			node_keys = generateNodeKeys(network_type, node_descriptors.length);
			nodes = new TreeSet<IChordRemoteReference>(new NodeComparator());
			
			IChordRemoteReference first = createFirstNode(node_descriptors[0], node_keys[0]);
			nodes.add(first);
		
			for (int node_index = 1; node_index < node_descriptors.length; node_index++) {

				IChordRemoteReference known_node = pickRandomElement(nodes);
				IKey key = node_keys[node_index];
		
				IChordRemoteReference next = createJoiningNode(node_descriptors[node_index], known_node, key);
				nodes.add(next);
			}
		}
	}

	public SortedSet<IChordRemoteReference> getNodes() {
		
		return nodes;
	}

	private IChordRemoteReference pickRandomElement(SortedSet<IChordRemoteReference> nodes) {
		
		int index = randomIndex(0, nodes.size());
		int count = 0;
		
		for (IChordRemoteReference reference : nodes) {
			if (count++ == index) return reference;
		}
		
		return null;
	}
	
	protected String getHost(NodeDescriptor node_descriptor) {
		
		return node_descriptor.ssh_client_wrapper.getServer().getHostName();
	}

	protected IChordRemoteReference createFirstNode(final NodeDescriptor node_descriptor, final IKey key) throws IOException, NotBoundException, SSH2Exception {
		
		ArgGen arg_gen = new ArgGen() {
			
			public List<String> getArgs(int local_port) {
				
				List<String> args = new ArrayList<String>();
				
				args.add("-s" + NetworkUtil.formatHostAddress(getHost(node_descriptor), local_port));
				addKeyArg(key, args);
				
				return args;
			}
		};
		
		return createNode(node_descriptor, arg_gen, StartRing.class);
	}

	protected IChordRemoteReference createJoiningNode(final NodeDescriptor node_descriptor, final IChordRemoteReference known_node, final IKey key) throws IOException, NotBoundException, SSH2Exception {
		
		ArgGen arg_gen = new ArgGen() {
			
			public List<String> getArgs(int local_port) throws RemoteException {
				
				List<String> args = new ArrayList<String>();
				
				args.add("-s" + NetworkUtil.formatHostAddress(getHost(node_descriptor), local_port));
				args.add("-k" + NetworkUtil.formatHostAddress(known_node.getRemote().getAddress())); 
				addKeyArg(key, args);
				
				return args;
			}
		};
		
		return createNode(node_descriptor, arg_gen, StartNode.class);
	}

	// Generates a random index such that lower <= index < upper.
	protected int randomIndex(int lower, int upper) {
		
		return lower + random.nextInt(upper - lower);
	}
	
	protected IKey[] generateNodeKeys(String network_type, int number_of_nodes) {
		
		IKey[] node_keys = new IKey[number_of_nodes];
		
		// Leave null entries for RANDOM since the keys will be generated from hashing addresses.
		if (!network_type.equals(RANDOM)) {
		
			BigInteger key_value = null;
			BigInteger node_increment = null;
	
			if (network_type.equals(EVEN)) {
				
				key_value = new BigInteger("0");
				node_increment = Key.KEYSPACE_SIZE.divide(new BigInteger(String.valueOf(number_of_nodes)));	
			}
			
			if (network_type.equals(CLUSTERED)) {
				
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

	protected void addKeyArg(IKey key, List<String> args) {
	
		if (key != null) args.add("-x" + key.toString(Key.DEFAULT_RADIX)); 
	}

	protected IChordRemoteReference bindToNode(String host, int port) throws TimeoutException {
				
		long start_time = System.currentTimeMillis();
		
		while (true) {
		
			try {

				Registry reg = LocateRegistry.getRegistry(host, port);
				IChordRemote remote = (IChordRemote) reg.lookup(IChordNode.CHORD_REMOTE_SERVICE);

				return new ChordRemoteReference(remote.getKey(), remote);
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

	private IChordRemoteReference createNode(NodeDescriptor node_descriptor, ArgGen arg_gen, Class<? extends AbstractServer> node_class) throws IOException, SSH2Exception {
		
		boolean finished = false;
		IChordRemoteReference node = null;
		
		while (!finished) {
			
			int port = next_port++;
		
			List<String> args = arg_gen.getArgs(port);
			
			Process p = runProcess(node_descriptor, node_class, args);
			
			try {
				node = bindToNode(getHost(node_descriptor), port);
				process_table.put(node, p);
				finished = true;
			}
			catch (TimeoutException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "timed out trying to connect to port: " + port);
			}
		}
		
		return node;
	}

	protected Process runProcess(NodeDescriptor node_descriptor, Class<? extends AbstractServer> node_class, List<String> args) throws IOException, SSH2Exception {
		
		return Processes.runJavaProcess(node_class, args, node_descriptor.ssh_client_wrapper, node_descriptor.java_version, node_descriptor.class_path);
	}
}
