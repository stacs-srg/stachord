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
import java.util.SortedSet;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.impl.SuccessorList;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;

/**
 * Class which provides the functionality to wait for a Chord ring to become complete - i.e. to fully stabilize. 
 * By calling waitForStableMethod with the complete set of nodes in the ring.
 *
 * @author Angus Macdonald (angus@cs.st-andrews.ac.uk)
 */
public class TestLogic {

	private static final double PROPORTION_TO_KILL = 0.2;

	private static final long DEATH_CHECK_INTERVAL = 2000;
	
	private static final int RANDOM_SEED = 32423545;
	
	private static final int WAIT_DELAY = 5000;

	/**
	 * Wait for the set of nodes in the ring to stabilize.
	 * @param nodes All of the nodes in the chord ring sorted in key order.
	 */
	public static void waitForStableRing(SortedSet<IChordRemoteReference> nodes) {
		
		while (!ringStable(nodes)) {
			sleep();
		}

		Diagnostic.trace(DiagnosticLevel.RUN, "ring is stable");
	}

	public static void waitForCompleteFingerTables(SortedSet<IChordRemoteReference> nodes) {
		
		while (!fingerTablesComplete(nodes)) sleep();

		Diagnostic.trace(DiagnosticLevel.RUN, "finger tables are complete");
	}

	public static void waitForCompleteSuccessorLists(SortedSet<IChordRemoteReference> nodes) {
		
		while (!successorListsComplete(nodes)) sleep();

		Diagnostic.trace(DiagnosticLevel.RUN, "successor lists are consistent");
	}

	public static void waitForCorrectRouting(SortedSet<IChordRemoteReference> nodes) {
		
		while (!routingCorrect(nodes)) sleep();

		Diagnostic.trace(DiagnosticLevel.RUN, "routing is correct");
	}

	/**
	 * Tests whether all nodes in the ring have correct predecessor and successor links and that none of the nodes in the ring have failed.
	 * 
	 * @return true if the ring is stable
	 */
	private static boolean ringStable(SortedSet<IChordRemoteReference> nodes) {
		
		try {
			IChordRemoteReference[] node_array = nodes.toArray(new IChordRemoteReference[]{});
			
			if (node_array.length == 1) {
				
				// Single-node ring, so stable if predecessor is null and successor is self.
				IChordRemote node = node_array[0].getRemote();
				return node.getPredecessor() == null && node.getSuccessor().getKey().equals(node.getKey());
			}
			else {
				for (int i = 0; i < nodes.size(); i++) {
	
					IChordRemote current = node_array[i].getRemote();
	
					if (current.getPredecessor() == null) return false;
	
					IChordRemoteReference predecessor = current.getPredecessor();
					IChordRemoteReference successor =   current.getSuccessor();
	
					if (!isNodeStable(current)) return false;
	
					// Check that the node's predecessor pointer refers to the same node as the preceding node in the key-ordered node list.
					if (i > 0) {
						if (!node_array[i - 1].getKey().equals(predecessor.getKey())) return false;
					}
					else {
						if (!node_array[node_array.length - 1].getKey().equals(predecessor.getKey())) return false;
					}
	
					// Check that the node's successor pointer refers to the same node as the next node in the key-ordered node list.
					if (i < node_array.length - 1) {
						if (!node_array[i + 1].getKey().equals(successor.getKey())) return false;
					}
					else {
						if (!node_array[0].getKey().equals(successor.getKey())) return false;
					}
				}
			}
		}
		catch (RemoteException e) {
			return false;
		}

		return true;
	}

	private static boolean isNodeStable(IChordRemote node) {

		return isSuccessorStable(node) && isPredecessorStable(node);
	}

	private static boolean isSuccessorStable(IChordRemote node) {

		try {
			IChordRemoteReference successor = node.getSuccessor();

			return successor != null && successor.getRemote().getPredecessor().getKey().equals(node.getKey());
		}
		catch (Exception e) {

			Diagnostic.trace(DiagnosticLevel.RUN, "error getting predecessor of successor");
			return false;
		}
	}

	private static boolean isPredecessorStable(IChordRemote node) {

		try {
			IChordRemoteReference predecessor = node.getPredecessor();

			return predecessor != null && predecessor.getRemote().getSuccessor().getKey().equals(node.getKey());
		}
		catch (Exception e) {

			Diagnostic.trace(DiagnosticLevel.RUN, "error getting successor of predecessor");
			return false;
		}
	}
	
	public static boolean fingerTablesComplete(SortedSet<IChordRemoteReference> nodes) {
		
		// Completeness criteria:
		// 1. The ring distance from a node's key to its fingers' keys never decreases going up the table.
		// 2. No finger table entry is null.

		for (IChordRemoteReference node : nodes) {

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
	
	public static boolean successorListsComplete(SortedSet<IChordRemoteReference> nodes) {
		
		IChordRemoteReference[] node_array = nodes.toArray(new IChordRemoteReference[]{});
		
		// Check the successor list of each node.
		for (int i = 0; i < nodes.size(); i++) {
			
			IChordRemote node = node_array[i].getRemote();

			List<IChordRemoteReference> successor_list;
			try {
				successor_list = node.getSuccessorList();

				// The length of the successor lists should be MIN(max_successor_list_length, number_of_nodes - 1).
				if (successor_list.size() != Math.min(SuccessorList.MAX_SUCCESSOR_LIST_SIZE, nodes.size() - 1)) {
					return false;
				}
	
				// Check that the successors of node with n'th key are n+1, n+2, n+3 etc, allowing for wrap-around.
				int expected_successor_index = i + 1;
	
				for (int j = 0; j < successor_list.size(); j++) {
	
					// Allow for wrap-around.
					if (expected_successor_index >= node_array.length) {
						expected_successor_index = 0;
					}
	
					if (!node_array[expected_successor_index].getKey().equals(successor_list.get(j).getKey())) {
						return false;
					}
	
					expected_successor_index++;
				}
			}
			catch (RemoteException e) { return false; }
		}
		
		return true;
	}
	
	public static boolean routingCorrect(SortedSet<IChordRemoteReference> nodes) {

		for (IChordRemoteReference node1 : nodes) {
			for (IChordRemoteReference node2 : nodes) {
				if (!routingCorrect(node1, node2, nodes.size())) {
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
				if (!expected_target.getKey().equals(lookupWithRetry(source, one_before_key).getKey())) return false;
			}
			else {
				if (!predecessor_of_target.getKey().equals(lookupWithRetry(source, one_before_key).getKey())) return false;
			}
	
			// Check that the target's own key routes to the target.
			if (!expected_target.getKey().equals(lookupWithRetry(source, expected_target.getKey()).getKey())) return false;
	
			// Check that a slightly larger key than the node's key routes to the node's successor.
			Key one_after_key = new Key(expected_target.getKey().keyValue().add(BigInteger.ONE));
			IChordRemote result_for_larger_key = lookupWithRetry(source, one_after_key);
	
			if (!successor_of_target.getKey().equals(result_for_larger_key.getKey())) return false;
		}
		catch (RemoteException e) { return false; }
		
		return true;
	}

	private static IChordRemote lookupWithRetry(IChordRemoteReference source, IKey key) throws RemoteException {
		
		while (true) {
			try {
				return source.getRemote().lookup(key).getRemote();
			}
			catch (RemoteException e) { sleep(); }
		}
	}
	
	private static int progress_count = 0;
	private static final int PROGRESS_COUNT_LINE_LENGTH = 80;
	
	private static void sleep() {
		
		System.out.print(".");
		progress_count++;
		if (progress_count == PROGRESS_COUNT_LINE_LENGTH) progress_count = 0;
		
		try { Thread.sleep(WAIT_DELAY); }
		catch (InterruptedException e) {}
	}

	public static void ringRecoversFromNodeFailure(INetwork network) throws IOException {
		
		waitForStableRing(network.getNodes());
		
		// Routing should still eventually work even in the absence of finger table maintenance.
		enableFingerTableMaintenance(network, false);
		
		killPartOfNetwork(network);
		
		waitForCorrectRouting(network.getNodes());
	
		// Turn on maintenance again.
		enableFingerTableMaintenance(network, true);
		
		waitForStableRing(network.getNodes());
	
		waitForCompleteFingerTables(network.getNodes());
	
		waitForCompleteSuccessorLists(network.getNodes());
		
		waitForCorrectRouting(network.getNodes());
		
		network.killAllNodes();
	}

	public static void enableFingerTableMaintenance(INetwork network, boolean enabled) throws RemoteException {
		
		for (IChordRemoteReference node : network.getNodes()) node.getRemote().enableFingerTableMaintenance(enabled);
	}

	public static void killPartOfNetwork(INetwork network) {
		
		IChordRemoteReference[] node_array = network.getNodes().toArray(new IChordRemoteReference[]{});
	
		int network_size = node_array.length;
		int number_to_kill = (int) Math.max(1, (int)(PROPORTION_TO_KILL * (double)network_size));
		
		Set<Integer> victim_indices = pickRandom(number_to_kill, network_size);
		
		for (int victim_index : victim_indices) {
			
			IChordRemoteReference victim = node_array[victim_index];			
			network.killNode(victim);
			
			// Wait for it to die.
			while (true) {
				try {
					victim.getRemote().isAlive();
					Thread.sleep(DEATH_CHECK_INTERVAL);
				}
				catch (RemoteException e) {
					break;
				}
				catch (InterruptedException e) { }
			}
		}
		
		assertEquals(network.getNodes().size(), network_size - number_to_kill);
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
