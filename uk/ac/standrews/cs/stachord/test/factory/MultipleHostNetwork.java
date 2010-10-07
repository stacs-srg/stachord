/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of stachord, an independent implementation of         *
 * the Chord protocol (http://pdos.csail.mit.edu/chord/).                  *
 *                                                                         *
 * stachord is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * stachord is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with stachord.  If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                         *
 ***************************************************************************/

package uk.ac.standrews.cs.stachord.test.factory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.UnknownPlatformException;
import uk.ac.standrews.cs.nds.util.ActionQueue;
import uk.ac.standrews.cs.nds.util.ActionWithNoResult;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Network comprising Chord nodes running on a set of specified physical machines running Linux or OSX.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class MultipleHostNetwork implements INetwork {
	
	private static final int QUEUE_MAX_THREADS =     10;               // The maximum degree of concurrency for check jobs.
	private static final int QUEUE_IDLE_TIMEOUT =  5000;               // The timeout for idle check job threads to die, in ms.
	
	private IKey[] node_keys;                                          // The keys of the nodes.
	private List<HostDescriptor> host_descriptors;                     // Handles to the nodes themselves.

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Needed for subclass {@link SingleHostNetwork}, but shouldn't be generally accessible.
	 */
	protected MultipleHostNetwork() {
	}
	
	/**
	 * Creates a new network.
	 * 
	 * @param host_descriptors a description of the target physical host for each Chord node to be created
	 * @param key_distribution the required key distribution
	 * 
	 * @throws IOException if the process for a node cannot be created
	 * @throws SSH2Exception if communication with a remote host fails
	 * @throws TimeoutException if one or more nodes cannot be instantiated within the timeout period
	 * @throws UnknownPlatformException if the operating system of one or more of the given hosts cannot be established
	 * @throws InterruptedException if there is an error during concurrent instantiation of the nodes
	 */
	public MultipleHostNetwork(final List<HostDescriptor> host_descriptors, KeyDistribution key_distribution) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException, InterruptedException {
		
		// Initialisation performed in separate method to allow subclass SingleMachineNetwork to catch SSH exception.
		init(host_descriptors, key_distribution);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public List<HostDescriptor> getNodes() {
		
		return host_descriptors;
	}

	public void killNode(HostDescriptor node) {
		
		synchronized (host_descriptors) {
			
			if (host_descriptors.contains(node)) {

				int network_size = host_descriptors.size();

				node.process.destroy();

				boolean successfully_removed = host_descriptors.remove(node);
				assert successfully_removed;

				assert host_descriptors.size() == network_size - 1;
			}
		}
	}

	public void killAllNodes() {
		
		synchronized (host_descriptors) {
			
			for (HostDescriptor node : host_descriptors) {

				node.process.destroy();
			}
			host_descriptors.clear();
		}
	}
	
	private static class ExceptionWrapper {
		Exception e;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	protected void init(final List<HostDescriptor> host_descriptors, KeyDistribution key_distribution) throws IOException, SSH2Exception, UnknownPlatformException, TimeoutException, InterruptedException {
		
		node_keys = generateNodeKeys(key_distribution, host_descriptors.size());
		this.host_descriptors = host_descriptors;
		
		ActionQueue actions = new ActionQueue(host_descriptors.size(), QUEUE_MAX_THREADS, QUEUE_IDLE_TIMEOUT);
		
		// Create one node first so that it can be used by the others to join the ring.
		HostDescriptor known_node_descriptor = host_descriptors.get(0);

		// Instantiate the new remote node and wait until a remote reference to it is established and stored in the host descriptor.
		ChordNodeFactory.createAndBindToRemoteNodeOnFreePort(known_node_descriptor, node_keys[0]);

		final IChordRemoteReference known_node = ((IChordRemoteReference)known_node_descriptor.application_reference);
		
		final ExceptionWrapper exception_wrapper = new ExceptionWrapper();

		for (int node_index = 1; node_index < host_descriptors.size(); node_index++) {

			final IKey key = node_keys[node_index];
			final HostDescriptor new_node_descriptor = host_descriptors.get(node_index);

			// Queue an asynchronous action to create the new node and bind to it.
			actions.enqueue(new ActionWithNoResult() {

				public void performAction() {

					try {
						
						// Instantiate the new remote node and wait until a remote reference to it is established and stored in the host descriptor.
						ChordNodeFactory.createAndBindToRemoteNodeOnFreePort(new_node_descriptor, key);
						
						IChordRemote new_node = ((IChordRemoteReference)new_node_descriptor.application_reference).getRemote();
						new_node.join(known_node);
					}
					catch (Exception e) {
						exception_wrapper.e = e;
					}
				}
			});
		}

		actions.blockUntilNoUncompletedActions();
		
		if (exception_wrapper.e instanceof IOException)              throw (IOException) exception_wrapper.e;
		if (exception_wrapper.e instanceof SSH2Exception)            throw (SSH2Exception) exception_wrapper.e;
		if (exception_wrapper.e instanceof TimeoutException)         throw (TimeoutException) exception_wrapper.e;
		if (exception_wrapper.e instanceof UnknownPlatformException) throw (UnknownPlatformException) exception_wrapper.e;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
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
}
