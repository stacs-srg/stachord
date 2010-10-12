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

package uk.ac.standrews.cs.stachord.test.recovery;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.impl.Constants;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.test.factory.INetwork;

/**
 * Core Chord test logic. In general, due to asynchrony we can't test much of interest immediately after a given operation.
 * Mostly all we can do is test that some condition eventually holds, by using a test that doesn't complete until the condition does hold.
 *
 * @author Angus Macdonald (angus@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class RecoveryTestLogic {

	/**
	 * The proportion of the nodes to be killed off in recovery tests.
	 */
	public static final double PROPORTION_TO_KILL = 0.2;

	private static final long DEATH_CHECK_INTERVAL = 2000;
	private static final int RANDOM_SEED = 32423545;
	private static final int WAIT_DELAY = 5000;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Tests the ability of the given network to recover from the killing off of a proportion ({@link #PROPORTION_TO_KILL}) of its nodes.
	 * The test performs the following sequence:
	 *
	 * <ol>
	 * <li>wait for the ring to stabilize</li>
	 * <li>disable finger table maintenance on all nodes</li>
	 * <li>kill off part of the network</li>
	 * <li>wait for routing to be performed correctly by the remaining nodes (which should happen even in the absence of finger table repair)</li>
	 * <li>re-enable finger table maintenance</li>
	 * <li>wait for the ring to stabilize</li>
	 * <li>wait for complete finger tables to be built on all nodes</li>
	 * <li>wait for complete successor lists to be built on all nodes</li>
	 * <li>wait for routing to be performed correctly by all nodes</li>
	 * </ol>
	 *
	 * @param network a Chord network
	 * @param test_timeout the timeout for individual steps of the test, in ms
	 *
	 * @throws RemoteException if an error occurs when setting finger table maintenance on a node
	 * @throws TimeoutException if one of the steps of the test is not completed within the timeout interval
	 */
	public static void testRingRecoveryFromNodeFailure(INetwork network, int test_timeout) throws RemoteException, TimeoutException {

		List<HostDescriptor> nodes = network.getNodes();

		try {
			System.out.println("waiting for stable ring... ");
			waitForStableRing(nodes, test_timeout);
			System.out.println("done");

			// Routing should still eventually work even in the absence of finger table maintenance.
			System.out.println("disabling finger table maintenance... ");
			enableFingerTableMaintenance(network, false);
			System.out.println("done");

			System.out.println("killing part of network... ");
			killPartOfNetwork(network);
			System.out.println("done");

			System.out.println("waiting for correct routing... ");
			waitForCorrectRouting(nodes, test_timeout);
			System.out.println("done");

			System.out.println("enabling finger table maintenance... ");
			// Turn on maintenance again.
			enableFingerTableMaintenance(network, true);
			System.out.println("done");

			System.out.println("waiting for stable ring... ");
			waitForStableRing(nodes, test_timeout);
			System.out.println("done");

			System.out.println("waiting for complete finger tables... ");
			waitForCompleteFingerTables(nodes, test_timeout);
			System.out.println("done");

			System.out.println("waiting for complete successor lists... ");
			waitForCompleteSuccessorLists(nodes, test_timeout);
			System.out.println("done");

			System.out.println("waiting for correct routing... ");
			waitForCorrectRouting(nodes, test_timeout);
			System.out.println("done");
		}
		catch (RemoteException e) {
			throw e;
		}
		catch (TimeoutException e) {
			throw e;
		}
		finally {

			System.out.println("killing remaining nodes... ");
			network.killAllNodes();
			System.out.println("done");
		}
	}

	/**
	 * Waits for each node in the ring to become stable. See {@link #ringStable(HostDescriptor, int)} for definition of stability.
	 *
	 * @param nodes a list of Chord nodes
	 * @param test_timeout the timeout interval, in ms
	 * @throws TimeoutException if the check is not completed within the timeout interval
	 */
	public static void waitForStableRing(List<HostDescriptor> nodes, int test_timeout) throws TimeoutException {

		checkWithTimeout(nodes, new IRingCheck() {

			@Override
			public boolean check(List<HostDescriptor> nodes) {
				return ringStable(nodes);
			}

		}, test_timeout);

		Diagnostic.trace(DiagnosticLevel.RUN, "ring is stable");
	}

	/**
	 * Waits for each node in the ring to have a complete finger table. See {@link #fingerTableComplete(HostDescriptor)} for definition of completeness.
	 *
	 * @param nodes a list of Chord nodes
	 * @param test_timeout the timeout interval, in ms
	 * @throws TimeoutException if the check is not completed within the timeout interval
	 */
	public static void waitForCompleteFingerTables(List<HostDescriptor> nodes, int test_timeout) throws TimeoutException {

		checkWithTimeout(nodes, new IRingCheck() {

			@Override
			public boolean check(List<HostDescriptor> nodes) {
				return fingerTableComplete(nodes);
			}

		}, test_timeout);

		Diagnostic.trace(DiagnosticLevel.RUN, "finger tables are complete");
	}

	/**
	 * Waits for each node in the ring to have a complete successor list. See {@link #successorListComplete(HostDescriptor, int)} for definition of completeness.
	 *
	 * @param nodes a list of Chord nodes
	 * @param test_timeout the timeout interval, in ms
	 * @throws TimeoutException if the check is not completed within the timeout interval
	 */
	public static void waitForCompleteSuccessorLists(List<HostDescriptor> nodes, int test_timeout) throws TimeoutException {

		checkWithTimeout(nodes, new IRingCheck() {

			@Override
			public boolean check(List<HostDescriptor> nodes) {
				return successorListComplete(nodes);
			}

		}, test_timeout);

		Diagnostic.trace(DiagnosticLevel.RUN, "successor lists are consistent");
	}

	/**
	 * Waits for each node in the ring to be able to route correctly to every other node. See {@link #routingCorrect(IChordRemoteReference, IChordRemoteReference)} for definition of correctness.
	 *
	 * @param nodes a list of Chord nodes
	 * @param test_timeout the timeout interval, in ms
	 * @throws TimeoutException if the check is not completed within the timeout interval
	 */
	public static void waitForCorrectRouting(List<HostDescriptor> nodes, int test_timeout) throws TimeoutException {

		checkWithTimeout(nodes, new IRingCheck() {

			@Override
			public boolean check(List<HostDescriptor> nodes) {
				return routingCorrect(nodes);
			}

		}, test_timeout);

		Diagnostic.trace(DiagnosticLevel.RUN, "routing is correct");
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Tests whether all nodes in are ring are stable. See {@link #ringStable(HostDescriptor, int)} for definition of stability.
	 *
	 * @param host_descriptors a list of Chord nodes
	 * @return true if all nodes are stable
	 */
	public static boolean ringStable(List<HostDescriptor> host_descriptors) {

		int ring_size = host_descriptors.size();

		for (HostDescriptor host_descriptor : host_descriptors) {
			if (!ringStable(host_descriptor, ring_size)) return false;
		}

		return true;
	}

	/**
	 * Tests whether the given node in a ring is stable. The stability criteria are:
	 *
	 * <ol>
	 * <li>the length of the cycle obtained by following successor pointers from the given node is equal to the size of the network</li>
	 * <li>the length of the cycle obtained by following predecessor pointers from the given node is equal to the size of the network</li>
	 * <li>no errors occur during the test</li>
	 * </ol>

	 *
	 * Stability is defined as the lengths of the successor and predecessor cycles both being equal
	 * to the number of nodes in the network. This is perhaps a bit weak as a test, since it doesn't guarantee that all nodes encountered in the cycle are actually part of this network.
	 *
	 * @param host_descriptor a Chord node
	 * @param network_size the known size of the network
	 * @return true if the node is stable.
	 */
	public static boolean ringStable(HostDescriptor host_descriptor, int network_size) {

		if (network_size == 1) {

			// Single-node ring, so stable if predecessor is null and successor is self.
			IChordRemoteReference application_reference = (IChordRemoteReference) host_descriptor.application_reference;
			IChordRemote node = application_reference.getRemote();

			try {
				return node.getPredecessor() == null && node.getSuccessor().getKey().equals(node.getKey());
			}
			catch (RemoteException e) {
				return false;
			}
		}

		// Check that we see cycles containing the same number of nodes as the network size.
		return (cycleLengthFrom(host_descriptor, true) == network_size && cycleLengthFrom(host_descriptor, false) == network_size);
	}

	/**
	 * Tests whether all nodes in the ring have complete finger tables. See {@link #fingerTableComplete(HostDescriptor)} for definition of completeness.
	 *
	 * @param host_descriptors a list of Chord nodes
	 * @return true if all nodes have complete finger tables
	 */
	public static boolean fingerTableComplete(List<HostDescriptor> host_descriptors) {

		for (HostDescriptor host_descriptor : host_descriptors) {
			if (!fingerTableComplete(host_descriptor)) return false;
		}

		return true;
	}

	/**
	 * Checks that the finger table of the given node is complete. The completeness criteria are:
	 *
	 * <ol>
	 * <li>the ring distance from a node's key to its fingers' keys never decreases going up the table</li>
	 * <li>no finger table entry is null</li>
	 * <li>no finger table entry refers to the node itself</li>
	 * <li>no errors occur during the test</li>
	 * </ol>
	 *
	 * @param host_descriptor node to be checked
	 * @return true if the finger table is complete
	 */
	public static boolean fingerTableComplete(HostDescriptor host_descriptor) {

		IChordRemoteReference node = (IChordRemoteReference) host_descriptor.application_reference;
		IChordRemoteReference previous_finger_reference = null;

		// For each finger...
		try {
			int finger_number = 0;

			for (IChordRemoteReference finger_reference : node.getRemote().getFingerList()) {

				// Check that the finger is not this node.
				if (finger_reference == null) return false;

				// Check that the finger is not closer in ring distance than the previous non-null finger.
				// Treat self-reference as the full ring distance, so ignore case where finger points to this node.
				if (previous_finger_reference != null && !finger_reference.getKey().equals(node.getKey()) &&
						node.getKey().firstCloserInRingThanSecond(finger_reference.getKey(), previous_finger_reference.getKey())) {

					return false;
				}

				previous_finger_reference = finger_reference;
				finger_number++;
			}
		}
		catch (RemoteException e) { return false; }

		return true;
	}

	/**
	 * Tests whether all nodes in the ring have complete successor lists. See {@link #successorListComplete(HostDescriptor, int)} for definition of completeness.
	 *
	 * @param host_descriptors a list of Chord nodes
	 * @return true if all nodes have complete successor lists
	 */
	public static boolean successorListComplete(List<HostDescriptor> host_descriptors) {

		int ring_size = host_descriptors.size();

		for (HostDescriptor host_descriptor : host_descriptors) {
			if (!successorListComplete(host_descriptor, ring_size)) return false;
		}

		return true;
	}

	/**
	 * Checks that the successor list of the given node is complete. The completeness criteria are:
	 *
	 * <ol>
	 * <li>the length of the list is {@link Constants#MAX_SUCCESSOR_LIST_SIZE} or one less than the size of the ring, whichever is less</li>
	 * <li>the entries in the list follow ring successor order</li>
	 * <li>no errors occur during the test</li>
	 * </ol>
	 *
	 * @param host_descriptor node to be checked
	 * @param network_size the known size of the network
	 * @return true if the successor list is complete
	 */
	public static boolean successorListComplete(HostDescriptor host_descriptor, int network_size) {

		IChordRemoteReference application_reference = (IChordRemoteReference) host_descriptor.application_reference;
		IChordRemote node = application_reference.getRemote();

		List<IChordRemoteReference> successor_list;
		try {
			successor_list = node.getSuccessorList();

			// The length of the successor lists should be MIN(max_successor_list_length, number_of_nodes - 1).
			if (successor_list.size() != Math.min(Constants.MAX_SUCCESSOR_LIST_SIZE, network_size - 1)) {
				return false;
			}

			// Check that the successors follow the node round the ring.
			IChordRemoteReference ring_node = node.getSuccessor();

			for (IChordRemoteReference successor_list_node : successor_list) {

				if (!successor_list_node.equals(ring_node)) return false;
				ring_node = ring_node.getRemote().getSuccessor();
			}
		}
		catch (RemoteException e) { return false; }

		return true;
	}

	/**
	 * Tests whether routing works correctly between all pairs of nodes. See {@link #routingCorrect(IChordRemoteReference, IChordRemoteReference)} for definition of correctness.
	 *
	 * @param host_descriptors a list of Chord nodes
	 * @return true if routing works correctly between all pairs of nodes
	 */
	public static boolean routingCorrect(List<HostDescriptor> host_descriptors) {

		for (HostDescriptor host_descriptor1 : host_descriptors) {
			for (HostDescriptor host_descriptor2 : host_descriptors) {

				IChordRemoteReference node1 = (IChordRemoteReference) host_descriptor1.application_reference;
				IChordRemoteReference node2 = (IChordRemoteReference) host_descriptor2.application_reference;

				if (!routingCorrect(node1, node2)) return false;
			}
		}
		return true;
	}

	/**
	 * Checks that routing works correctly from one given node to another. The correctness criteria are:
	 *
	 * <ol>
	 * <li>routing to the key one less than the target's key should result in the target, unless the target has a predecessor with a key one less than it</li>
	 * <li>routing to the target's own key should result in the target</li>
	 * <li>routing to the key one more than the target's key should result in the target's successor</li>
	 * </ol>
	 *
	 * @param source the node whose routing is being tested
	 * @param target the node to which routing is being tested
	 * @return true if routing works correctly
	 */
	public static boolean routingCorrect(IChordRemoteReference source, IChordRemoteReference target) {

		try {
			return routingToSmallerKeyCorrect(source, target) &&
			       routingToSameKeyCorrect(source, target) &&
			       routingToLargerKeyCorrect(source, target);
		}
		catch (RemoteException e) { return false; }
	}

	/**
	 * Traverses the ring from the given node in the given direction, and returns the length of the cycle containing the given node, or zero if there is no such cycle.
	 *
	 * @param host_descriptor a ring node
	 * @param forwards true if the ring should be traversed via successor pointers, false if it should be traversed via predecessor pointers
	 * @return the length of the cycle containing the given node, or zero if the ring node is null or there is no such cycle.
	 */
	public static int cycleLengthFrom(HostDescriptor host_descriptor, boolean forwards) {

		if (host_descriptor.application_reference == null) return 0;

		// Record the nodes that have already been encountered.
		Set<IChordRemoteReference> nodes_encountered = new HashSet<IChordRemoteReference>();

		int cycle_length = 0;

		IChordRemoteReference node = (IChordRemoteReference) host_descriptor.application_reference;

		while (true) {

			cycle_length ++;

			try {
				node = forwards ? node.getRemote().getSuccessor() : node.getRemote().getPredecessor();
			}
			catch (RemoteException e) {
				return 0;
			}

			if (node.equals(host_descriptor.application_reference)) return cycle_length;

			// If the node is not the start node but has already been encountered, there is a cycle but it doesn't contain the start node.
			if (nodes_encountered.contains(node)) return 0;

			nodes_encountered.add(node);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static boolean routingToSmallerKeyCorrect(IChordRemoteReference source, IChordRemoteReference target) throws RemoteException {

		// Check that a slightly smaller key than the target's key routes to the node, except
		// in the pathological case where the target has a predecessor with a key one less than it.
		IChordRemoteReference predecessor_of_target = target.getRemote().getPredecessor();

		Key one_before_key = new Key(target.getKey().keyValue().subtract(BigInteger.ONE));
		boolean pathological = predecessor_of_target != null && predecessor_of_target.getKey().equals(one_before_key);

		IChordRemote result_for_smaller_key = lookup(source, one_before_key);

		return (!pathological && result_for_smaller_key.getKey().equals(target.getKey())) ||
		       ( pathological && result_for_smaller_key.getKey().equals(predecessor_of_target.getKey()));
	}

	private static boolean routingToSameKeyCorrect(IChordRemoteReference source, IChordRemoteReference target) throws RemoteException {

		// Check that the target's own key routes to the target.
		IChordRemote result_for_same_key = lookup(source, target.getKey());
		return target.getKey().equals(result_for_same_key.getKey());
	}

	private static boolean routingToLargerKeyCorrect(IChordRemoteReference source, IChordRemoteReference target) throws RemoteException {

		// Check that a slightly larger key than the node's key routes to the node's successor.
		IChordRemoteReference successor_of_target = target.getRemote().getSuccessor();

		Key one_after_target_key = new Key(target.getKey().keyValue().add(BigInteger.ONE));
		IChordRemote result_for_larger_key = lookup(source, one_after_target_key);

		return result_for_larger_key.getKey().equals(successor_of_target.getKey());
	}

	private static IChordRemote lookup(IChordRemoteReference source, IKey key) throws RemoteException {

		return source.getRemote().lookup(key).getRemote();
	}

	private static void sleep() {

		try { Thread.sleep(WAIT_DELAY); }
		catch (InterruptedException e) {}
	}

	private static void enableFingerTableMaintenance(INetwork network, boolean enabled) throws RemoteException {

		for (HostDescriptor machine_descriptor : network.getNodes()) {
			IChordRemoteReference application_reference = (IChordRemoteReference) machine_descriptor.application_reference;
			application_reference.getRemote().enablePeerStateMaintenance(enabled);
		}
	}

	private static void killPartOfNetwork(INetwork network) {

		List<HostDescriptor> nodes = network.getNodes();
		int network_size = nodes.size();

		// No point in killing off the only member of the network and expecting it to recover.
		if (network_size > 1) {
			int number_to_kill = Math.max(1, (int)(PROPORTION_TO_KILL * network_size));

			Set<Integer> victim_indices = pickRandom(number_to_kill, network_size);

			for (int victim_index : victim_indices) {

				HostDescriptor victim = nodes.get(victim_index);
				network.killNode(victim);

				IChordRemoteReference application_reference = (IChordRemoteReference) victim.application_reference;

				// Wait for it to die.
				while (true) {
					try {
						application_reference.getRemote().isAlive();
						Thread.sleep(DEATH_CHECK_INTERVAL);
					}
					catch (RemoteException e) {
						break;
					}
					catch (InterruptedException e) { }
				}
			}

			assertEquals(nodes.size(), network_size - number_to_kill);
		}
	}

	/**
	 * Returns a randomly selected subset of integers in a given range.
	 *
	 * @param number_to_select
	 * @param range
	 * @return a set of size number_to_select containing integers from 0 to range-1 inclusive
	 */
	private static Set<Integer> pickRandom(int number_to_select, int range) {

		Set<Integer> set = new HashSet<Integer>();
		Random random = new Random(RANDOM_SEED);

		for (int i = 0; i < number_to_select; i++) {
			int choice = -1;
			while (choice == -1 || set.contains(choice)) {
				choice = random.nextInt(range);
			}
			set.add(choice);
		}

		return set;
	}

	private static void checkWithTimeout(List<HostDescriptor> nodes, IRingCheck checker, int test_timeout) throws TimeoutException {

		long start_time = System.currentTimeMillis();
		boolean timed_out = false;
		DiagnosticLevel previous_level = Diagnostic.getLevel();

		while (!checker.check(nodes)) {

			if (timed_out) {

				dumpState(nodes);
				throw new TimeoutException();
			}

			if (timeElapsed(start_time) > test_timeout) {

				// Exceeded timeout. Go round loop one more time with full diagnostics, then throw exception.
				System.out.println("\n>>>>>>>>>>>>>>>> Potential timeout: executing one more check with full diagnostics\n");
				timed_out = true;
				Diagnostic.setLevel(DiagnosticLevel.FULL);
			}
			sleep();
		}

		if (timed_out) {
			System.out.println("\n>>>>>>>>>>>>>>>> Succeeded on last check\n");
			Diagnostic.setLevel(previous_level);
		}
	}

	private static long timeElapsed(long start_time) {

		return System.currentTimeMillis() - start_time;
	}

	private static void dumpState(List<HostDescriptor> nodes) {

		System.out.println("\n>>>>>>>>>>>>>>>> Test timed out: dumping state\n");

		for (HostDescriptor machine_descriptor : nodes) {

			System.out.println(machine_descriptor);

			try {
				IChordRemoteReference application_reference = (IChordRemoteReference) machine_descriptor.application_reference;
				System.out.println(application_reference.getRemote().toStringDetailed());
			}
			catch (RemoteException e) {
				System.out.println("application inaccessible");
			}
			System.out.println();
		}

		System.out.println("\n>>>>>>>>>>>>>>>> End of state\n");
	}

	private interface IRingCheck {

		boolean check(List<HostDescriptor> nodes);

	}
}
