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

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.ActionQueue;
import uk.ac.standrews.cs.nds.util.ActionWithNoResult;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.remote_management.server.HostDescriptor;
import uk.ac.standrews.cs.remote_management.server.UnknownPlatformException;
import uk.ac.standrews.cs.remote_management.util.ProcessInvocation;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.servers.AbstractServer;
import uk.ac.standrews.cs.stachordRMI.servers.StartNode;
import uk.ac.standrews.cs.stachordRMI.servers.StartRing;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Network comprising Chord nodes running on a set of specified physical machines running Linux or OSX.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class MultipleMachineNetwork implements INetwork {
	
	private static int next_port = 54496;                              // The next port to be used; static to allow multiple concurrent networks.
	private static final Object sync = new Object();                   // Used for serializing network creation.
	
	private static final int REGISTRY_RETRY_INTERVAL =    2000;        // Retry connecting to remote nodes at 2s intervals.
	private static final int REGISTRY_TIMEOUT_INTERVAL = 20000;        // Give up after 20s.
	
	private static final int QUEUE_MAX_THREADS =     10;               // The maximum degree of concurrency for check jobs.
	private static final int QUEUE_IDLE_TIMEOUT =  5000;               // The timeout for idle check job threads to die, in ms.

	private IKey[] node_keys;                                          // The keys of the nodes.
	private List<HostDescriptor> nodes;      // The nodes themselves.

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
	 * @throws TimeoutException 
	 * @throws UnknownPlatformException 
	 */
	public MultipleMachineNetwork(final List<HostDescriptor> node_descriptors, KeyDistribution key_distribution) throws IOException, SSH2Exception, InterruptedException, TimeoutException, UnknownPlatformException {
		
		// Initialisation performed in separate method to allow subclass SingleMachineNetwork to catch SSH exception.
		init(node_descriptors, key_distribution);
	}

	protected void init(final List<HostDescriptor> node_descriptors, KeyDistribution key_distribution) throws IOException, SSH2Exception, InterruptedException, TimeoutException, UnknownPlatformException {
			
		node_keys = generateNodeKeys(key_distribution, node_descriptors.size());
		nodes = node_descriptors;
		
		ActionQueue actions = new ActionQueue(node_descriptors.size(), QUEUE_MAX_THREADS, QUEUE_IDLE_TIMEOUT);
		
		final HostDescriptor known_node = node_descriptors.get(0);
		createFirstNode(known_node, node_keys[0]);

		for (int node_index = 1; node_index < node_descriptors.size(); node_index++) {

			final int index = node_index;

			actions.enqueue(new ActionWithNoResult() {

				public void performAction() {

					try {

						IKey key = node_keys[index];

						createJoiningNode(node_descriptors.get(index), known_node, key);

					} catch (Exception e) {
						ErrorHandling.exceptionError(e);
					}
				}
			});
		}

		actions.blockUntilNoUncompletedActions();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public List<HostDescriptor> getNodes() {
		
		return nodes;
	}

	public void killNode(HostDescriptor node) {
		
		synchronized (nodes) {
			
			if (nodes.contains(node)) {

				int network_size = nodes.size();

				node.process.destroy();

				boolean successfully_removed = nodes.remove(node);
				assert successfully_removed;

				assert nodes.size() == network_size - 1;
			}
		}
	}

	public void killAllNodes() {
		
		synchronized (nodes) {
			
			for (HostDescriptor node : nodes) {

				node.process.destroy();
			}
			nodes.clear();
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static void createFirstNode(final HostDescriptor host_descriptor, int port) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {
		
		ArgGen arg_gen = new ArgGen() {
			
			public List<String> getArgs(int local_port) {
				
				List<String> args = new ArrayList<String>();
				
				args.add("-s" + NetworkUtil.formatHostAddress(host_descriptor.host, local_port));
				
				return args;
			}
		};
		
		createNodeProcessAndBindToApplication(host_descriptor, port, arg_gen, StartRing.class);
	}

	public static void createFirstNode(final HostDescriptor host_descriptor, int port, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {
		
		ArgGen arg_gen = new ArgGen() {
			
			public List<String> getArgs(int local_port) {
				
				List<String> args = new ArrayList<String>();
				
				args.add("-s" + NetworkUtil.formatHostAddress(host_descriptor.host, local_port));
				addKeyArg(key, args);
				
				return args;
			}
		};
		
		createNodeProcessAndBindToApplication(host_descriptor, port, arg_gen, StartRing.class);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static Process runProcess(HostDescriptor host_descriptor, Class<? extends AbstractServer> node_class, List<String> args) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {
		
		if (host_descriptor.ssh_client_wrapper != null) {
			if (host_descriptor.application_urls != null) {
				return ProcessInvocation.runJavaProcess(node_class, args, host_descriptor.ssh_client_wrapper, host_descriptor.application_urls, true);
			}
			else {
				return ProcessInvocation.runJavaProcess(node_class, args, host_descriptor.ssh_client_wrapper, host_descriptor.class_path);
			}
		}
		else {
			return ProcessInvocation.runJavaProcess(node_class, args);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	private interface ArgGen {
		List<String> getArgs(int local_port);
	}

	private static void createFirstNode(final HostDescriptor machine_descriptor, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {
		
		ArgGen arg_gen = new ArgGen() {
			
			public List<String> getArgs(int local_port) {
				
				List<String> args = new ArrayList<String>();
				
				args.add("-s" + NetworkUtil.formatHostAddress(machine_descriptor.host, local_port));
				addKeyArg(key, args);
				
				return args;
			}
		};
		
		createNode(machine_descriptor, arg_gen, StartRing.class);
	}
	
	private static void createJoiningNode(final HostDescriptor machine_descriptor, final HostDescriptor known_node, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {
		
		ArgGen arg_gen = new ArgGen() {
			
			public List<String> getArgs(int local_port) {
				
				List<String> args = new ArrayList<String>();
				
				args.add("-s" + NetworkUtil.formatHostAddress(machine_descriptor.host, local_port));
				args.add("-k" + NetworkUtil.formatHostAddress(known_node.host, known_node.port)); 
				addKeyArg(key, args);
				
				return args;
			}
		};
		
		createNode(machine_descriptor, arg_gen, StartNode.class);
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

	private static IChordRemoteReference bindToNode(HostDescriptor node_descriptor) throws TimeoutException {
				
		long start_time = System.currentTimeMillis();
		
		while (true) {

			try {
				return ChordNodeImpl.bindToNode(NetworkUtil.getInetSocketAddress(node_descriptor.host, node_descriptor.port));
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

	private static void createNode(HostDescriptor node_descriptor, ArgGen arg_gen, Class<? extends AbstractServer> node_class) throws IOException, SSH2Exception, UnknownPlatformException {
		
		boolean finished = false;
		
		while (!finished) {
			
			int port = 0;
			
			synchronized (sync) {
				port = next_port++;
			}

			node_descriptor.port = port;
		
			List<String> args = arg_gen.getArgs(port);
			
			try {
				node_descriptor.process = runProcess(node_descriptor, node_class, args);
				node_descriptor.application_reference = bindToNode(node_descriptor);
				finished = true;
			}
			catch (TimeoutException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "timed out trying to connect to port: " + port);
			}
		}
	}

	private static void createNodeProcessAndBindToApplication(HostDescriptor node_descriptor, int port, ArgGen arg_gen, Class<? extends AbstractServer> node_class) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

		createNodeProcess(node_descriptor, port, arg_gen, node_class);

		node_descriptor.application_reference = bindToNode(node_descriptor);
	}

	private static void createNodeProcess(HostDescriptor node_descriptor, int port, ArgGen arg_gen, Class<? extends AbstractServer> node_class) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {
		
		List<String> args = arg_gen.getArgs(port);

		node_descriptor.process = runProcess(node_descriptor, node_class, args);
		node_descriptor.port = port;
	}
}
