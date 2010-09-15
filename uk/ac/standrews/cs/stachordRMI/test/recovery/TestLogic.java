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
package uk.ac.standrews.cs.stachordRMI.test.recovery;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.remote_management.server.HostDescriptor;
import uk.ac.standrews.cs.stachordRMI.impl.SuccessorList;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;

/**
 *
 * @author Angus Macdonald (angus@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class TestLogic {

	private static final double PROPORTION_TO_KILL = 0.2;
	private static final long DEATH_CHECK_INTERVAL = 2000;
	private static final int RANDOM_SEED = 32423545;
	private static final int WAIT_DELAY = 5000;

	/**
	 * Wait for the set of nodes in the ring to stabilize.
	 * @param nodes All of the nodes in the chord ring sorted in key order.
	 * @throws TimeoutException 
	 */
	public static void checkWithTimeout(List<HostDescriptor> nodes, IRingCheck checker, int test_timeout) throws TimeoutException {
		
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

	public static void waitForStableRing(List<HostDescriptor> nodes, int test_timeout) throws TimeoutException {
		
		checkWithTimeout(nodes, new IRingCheck() {

			@Override
			public boolean check(List<HostDescriptor> nodes) {
				return ringStable(nodes);
			}
			
		}, test_timeout);

		Diagnostic.trace(DiagnosticLevel.RUN, "ring is stable");
	}

	public static void waitForCompleteFingerTables(List<HostDescriptor> nodes, int test_timeout) throws TimeoutException {
		
		checkWithTimeout(nodes, new IRingCheck() {

			@Override
			public boolean check(List<HostDescriptor> nodes) {
				return fingerTablesComplete(nodes);
			}
			
		}, test_timeout);

		Diagnostic.trace(DiagnosticLevel.RUN, "finger tables are complete");
	}

	public static void waitForCompleteSuccessorLists(List<HostDescriptor> nodes, int test_timeout) throws TimeoutException {
		
		checkWithTimeout(nodes, new IRingCheck() {

			@Override
			public boolean check(List<HostDescriptor> nodes) {
				return successorListsComplete(nodes);
			}
			
		}, test_timeout);

		Diagnostic.trace(DiagnosticLevel.RUN, "successor lists are consistent");
	}

	public static void waitForCorrectRouting(List<HostDescriptor> nodes, int test_timeout) throws TimeoutException {
		
		checkWithTimeout(nodes, new IRingCheck() {

			@Override
			public boolean check(List<HostDescriptor> nodes) {
				return routingCorrect(nodes);
			}
			
		}, test_timeout);

		Diagnostic.trace(DiagnosticLevel.RUN, "routing is correct");
	}

	/**
	 * Tests whether all nodes in the ring have correct predecessor and successor links and that none of the nodes in the ring have failed.
	 * 
	 * @return true if the ring is stable
	 */
	private static boolean ringStable(List<HostDescriptor> nodes) {
		
		try {
			
			if (nodes.size() == 1) {
				
				// Single-node ring, so stable if predecessor is null and successor is self.
				IChordRemoteReference application_reference = (IChordRemoteReference) nodes.get(0).application_reference;
				IChordRemote node = application_reference.getRemote();
				return node.getPredecessor() == null && node.getSuccessor().getKey().equals(node.getKey());
			}
			else {
				for (HostDescriptor node : nodes) {
					if (cycleLengthFrom(node, true) != nodes.size() || cycleLengthFrom(node, false) != nodes.size()) return false;
				}
			}
		}
		catch (RemoteException e) {
			return false;
		}

		return true;
	}

	/**
	 * Traverses the ring from the given node in the given direction, and returns the length of the cycle containing the given node, or zero if there is no such cycle.
	 * 
	 * @param node a ring node
	 * @param forwards true if the ring should be traversed via successor pointers, false if it should be traversed via predecessor pointers
	 * @return the length of the cycle containing the given node, or zero if there is no such cycle.
	 */
	private static int cycleLengthFrom(HostDescriptor start_node, boolean forwards) {

		// Record the nodes that have already been encountered.
		Set<IChordRemoteReference> nodes_encountered = new HashSet<IChordRemoteReference>();
		
		int cycle_length = 0;
		
		IChordRemoteReference node = (IChordRemoteReference) start_node.application_reference;
		
		while (true) {
			
			cycle_length ++;
			
			try {
				node = forwards ? node.getRemote().getSuccessor() : node.getRemote().getPredecessor();
			}
			catch (RemoteException e) {
				return 0;
			}
			
			if (node.equals(start_node.application_reference)) return cycle_length;
			
			// If the node is not the start node but has already been encountered, there is a cycle but it doesn't contain the start node.
			if (nodes_encountered.contains(node)) return 0;
			
			nodes_encountered.add(node);
		}
	}
	
	public static boolean fingerTablesComplete(List<HostDescriptor> nodes) {
		
		// Completeness criteria:
		// 1. The ring distance from a node's key to its fingers' keys never decreases going up the table.
		// 2. No finger table entry is null.

		for (HostDescriptor machine_descriptor : nodes) {

			IChordRemoteReference node = (IChordRemoteReference) machine_descriptor.application_reference;
			IChordRemoteReference previous_finger_reference = null;

			// For each finger...
			try {
				int finger_number = 0;

				for (IChordRemoteReference finger_reference : node.getRemote().getFingerList()) {
					
					// Check that the finger is not this node.
					if (finger_reference == null) return false; // { System.out.println("afc1"); return false;}
					else {

						// Check that the finger is not closer in ring distance than the previous non-null finger.
						// Treat self-reference as the full ring distance, so ignore case where finger points to this node.
						if (previous_finger_reference != null && !finger_reference.getKey().equals(node.getKey()) &&
								node.getKey().firstCloserInRingThanSecond(finger_reference.getKey(), previous_finger_reference.getKey())) {
							
							return false;
						}
						
						previous_finger_reference = finger_reference;
					}
					finger_number++;
				}
			}
			catch (RemoteException e) { return false; }
		}
		
		return true;
	}
	
	public static boolean successorListsComplete(List<HostDescriptor> nodes) {
		
		// Check the successor list of each node.
		for (HostDescriptor machine_descriptor : nodes) {
			
			IChordRemoteReference application_reference = (IChordRemoteReference) machine_descriptor.application_reference;
			IChordRemote node = application_reference.getRemote();

			List<IChordRemoteReference> successor_list;
			try {
				successor_list = node.getSuccessorList();

				// The length of the successor lists should be MIN(max_successor_list_length, number_of_nodes - 1).
				if (successor_list.size() != Math.min(SuccessorList.MAX_SUCCESSOR_LIST_SIZE, nodes.size() - 1)) {
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
		}
		
		return true;
	}
	
	public static boolean routingCorrect(List<HostDescriptor> nodes) {

		for (HostDescriptor machine_descriptor1 : nodes) {
			for (HostDescriptor machine_descriptor2 : nodes) {
				
				IChordRemoteReference application_reference1 = (IChordRemoteReference) machine_descriptor1.application_reference;
				IChordRemoteReference application_reference2 = (IChordRemoteReference) machine_descriptor2.application_reference;
				
				if (!routingCorrect(application_reference1, application_reference2, nodes.size())) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean routingCorrect(IChordRemoteReference source, IChordRemoteReference expected_target, int ring_size) {

		try {
			IChordRemoteReference predecessor_of_target = expected_target.getRemote().getPredecessor();
			IChordRemoteReference successor_of_target = expected_target.getRemote().getSuccessor();
			
			// Check that a slightly smaller key than the target's key routes to the node, except
			// in the pathological case where the target has a predecessor with a key one less than it.
			Key one_before_key = new Key(expected_target.getKey().keyValue().subtract(BigInteger.ONE));
			
			if (predecessor_of_target == null || !predecessor_of_target.getKey().equals(one_before_key)) {
				if (!expected_target.getKey().equals(lookup(source, one_before_key).getKey())) return false;
			}
			else {
				if (!predecessor_of_target.getKey().equals(lookup(source, one_before_key).getKey())) return false;
			}
	
			// Check that the target's own key routes to the target.
			if (!expected_target.getKey().equals(lookup(source, expected_target.getKey()).getKey())) return false;
	
			// Check that a slightly larger key than the node's key routes to the node's successor.
			Key one_after_key = new Key(expected_target.getKey().keyValue().add(BigInteger.ONE));
			IChordRemote result_for_larger_key = lookup(source, one_after_key);
	
			if (!successor_of_target.getKey().equals(result_for_larger_key.getKey())) return false;
		}
		catch (RemoteException e) { return false; }
		
		return true;
	}

	private static IChordRemote lookup(IChordRemoteReference source, IKey key) throws RemoteException {
		
		return source.getRemote().lookup(key).getRemote();
	}
	

	
	private static void sleep() {
		
		try { Thread.sleep(WAIT_DELAY); }
		catch (InterruptedException e) {}
	}

	public static void ringRecoversFromNodeFailure(INetwork network, int test_timeout) throws IOException, TimeoutException {
		
		List<HostDescriptor> nodes = network.getNodes();
		
		try {
			System.out.println("waiting for stable ring... ");
			waitForStableRing(nodes, test_timeout);
			System.out.println("done");
			
			System.out.println("disabling finger table maintenance... ");
			// Routing should still eventually work even in the absence of finger table maintenance.
			enableFingerTableMaintenance(network, false);
			System.out.println("done");
			
			System.out.println("killing part of network... ");
			killPartOfNetwork(network, test_timeout);
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
		catch (IOException e) {
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

	public static void enableFingerTableMaintenance(INetwork network, boolean enabled) throws RemoteException {
		
		for (HostDescriptor machine_descriptor : network.getNodes()) {
			IChordRemoteReference application_reference = (IChordRemoteReference) machine_descriptor.application_reference;
			application_reference.getRemote().enableFingerTableMaintenance(enabled);
		}
	}

	public static void killPartOfNetwork(INetwork network, int test_timeout) {
		
		List<HostDescriptor> nodes = network.getNodes();
		int network_size = nodes.size();
		
		// No point in killing off the only member of the network and expecting it to recover.
		if (network_size > 1) {
			int number_to_kill = (int) Math.max(1, (int)(PROPORTION_TO_KILL * (double)network_size));
			
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
	 * @param number_to_select
	 * @param range
	 * @return a set of size number_to_select containing integers from 0 to range-1 inclusive
	 */
	private static Set<Integer> pickRandom(int number_to_select, int range) {
		
		Set<Integer> set = new HashSet<Integer>();
		Random random = new Random(RANDOM_SEED);
		
		for (int i = 0; i < number_to_select; i++) {
			int choice = -1;
			while (choice == -1 || set.contains(choice))
				choice = random.nextInt(range);
			set.add(choice);
		}
		
		return set;
	}
}
