package uk.ac.standrews.cs.stachordRMI.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.List;
import java.util.SortedSet;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.impl.SuccessorList;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

/**
 * Class which provides the functionality to wait for a Chord ring to become complete - i.e. to fully stabilize. 
 * By calling waitForStableMethod with the complete set of nodes in the ring.
 *
 * @author Angus Macdonald (angus@cs.st-andrews.ac.uk)
 */
public class TestLogic {

	private static final int WAIT_DELAY = 5000;
	


	/**
	 * Wait for the set of nodes in the ring to stabilize. The {@link #uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode.stabilize()} operation will be called on each
	 * node until this happens.
	 * @param nodes All of the nodes in the chord ring sorted in key order.
	 */
	public static void waitForStableRing(SortedSet<IChordRemote> nodes) {
		
		while (!ringStable(nodes)) {
//			System.out.println("not stable yet...");
			sleep();
		}

		Diagnostic.trace(DiagnosticLevel.RUN, "ring is stable");
	}

	public static void waitForCompleteFingerTables(SortedSet<IChordRemote> nodes) {
		
		while (!fingerTablesComplete(nodes)) sleep();

		Diagnostic.trace(DiagnosticLevel.RUN, "finger tables are complete");
	}

	public static void waitForCompleteSuccessorLists(SortedSet<IChordRemote> nodes) {
		
		while (!successorListsComplete(nodes)) sleep();

		Diagnostic.trace(DiagnosticLevel.RUN, "successor lists are consistent");
	}

	public static void waitForCorrectRouting(SortedSet<IChordRemote> nodes) {
		
		while (!routingCorrect(nodes)) sleep();

		Diagnostic.trace(DiagnosticLevel.RUN, "routing is correct");
	}

	/**
	 * Tests whether all nodes in the ring have correct predecessor and successor links and that none of the nodes in the ring have failed.
	 * 
	 * @return true if the ring is stable
	 */
	private static boolean ringStable(SortedSet<IChordRemote> nodes) {
		
		try {
			IChordRemote[] node_array = nodes.toArray(new IChordRemote[]{});
			
			if (node_array.length == 1) {
				
				// Single-node ring, so stable if predecessor is null and successor is self.
				IChordRemote node = node_array[0];
				return node.getPredecessor() == null && node.getSuccessor().getKey().equals(node.getKey());
			}
			else {
				for (int i = 0; i < nodes.size(); i++) {
	
					IChordRemote current = node_array[i];
	
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
	
	public static boolean fingerTablesComplete(SortedSet<IChordRemote> nodes) {
		
		// Completeness criteria:
		// 1. The ring distance from a node's key to its fingers' keys never decreases going up the table.
		// 2. No finger table entry is null.

		for (IChordRemote node : nodes) {

			IChordRemoteReference previous_finger_reference = null;

			// For each finger...
			try {
				int finger_number = 0;
//				System.out.println("\n\n");
//				System.out.println("node: " + node.getKey());
				for (IChordRemoteReference finger_reference : node.getFingerList()) {
					
//					System.out.println("finger number: " + finger_number);
//					System.out.println("finger reference: " + finger_reference == null ? "null" : finger_reference.getKey());

					// Check that the finger is not this node.
					if (finger_reference == null) return false; // { System.out.println("afc1"); return false;}
					else {

						// Check that the finger is not closer in ring distance than the previous non-null finger.
						// Treat self-reference as the full ring distance, so ignore case where finger points to this node.
						if (previous_finger_reference != null && !finger_reference.getKey().equals(node.getKey()) && node.getKey().firstCloserInRingThanSecond(finger_reference.getKey(), previous_finger_reference.getKey())) {
							
//							System.out.println("afc2"); 
//							
//							System.out.println("previous finger reference: " + previous_finger_reference.getKey());
							
							
							return false;}
						
						previous_finger_reference = finger_reference;
					}
					finger_number++;
				}
			}
			catch (RemoteException e) { return false; } // { { System.out.println("afc3"); return false;} }
		}
		
		return true;
	}
	
	public static boolean successorListsComplete(SortedSet<IChordRemote> nodes) {
		
		IChordRemote[] node_array = nodes.toArray(new IChordRemote[]{});
		
		// Check the successor list of each node.
		for (int i = 0; i < nodes.size(); i++) {
			
			IChordRemote node = node_array[i];
			
//			System.out.println("checking node: " + i);

			List<IChordRemoteReference> successor_list;
			try {
				successor_list = node.getSuccessorList();
				
//				System.out.println("successor list: " + successor_list);

				// The length of the successor lists should be MIN(max_successor_list_length, number_of_nodes - 1).
				if (successor_list.size() != Math.min(SuccessorList.MAX_SUCCESSOR_LIST_SIZE, nodes.size() - 1)) {
//					System.out.println("asc1");
//					System.out.println("successor_list.size: " + successor_list.size());
//					System.out.println("nodes.size: " + nodes.size());
					return false;
				}
	
				// Check that the successors of node with n'th key are n+1, n+2, n+3 etc, allowing for wrap-around.
				int expected_successor_index = i + 1;
	
				for (int j = 0; j < successor_list.size(); j++) {
	
					// Allow for wrap-around.
					if (expected_successor_index >= node_array.length) {
						expected_successor_index = 0;
					}
	
					if(!node_array[expected_successor_index].getKey().equals(successor_list.get(j).getKey())) {
//						System.out.println("asc2");
						return false;
					}
	
					expected_successor_index++;
				}
			}
			catch (RemoteException e) { 
//				System.out.println("asc3");
return false; }
		}
		
		return true;
	}
	
	public static boolean routingCorrect(SortedSet<IChordRemote> nodes) {

		for (IChordRemote node1 : nodes) {
			for (IChordRemote node2 : nodes) {
				if (!routingCorrect(node1, node2, nodes.size())) return false;
			}
		}
		return true;
	}

	private static boolean routingCorrect(IChordRemote source, IChordRemote expected_target, int ring_size) {

		try {
			IChordRemoteReference predecessor_of_target = expected_target.getPredecessor();
			IChordRemoteReference successor_of_target = expected_target.getSuccessor();
			
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
		catch (RemoteException e) {
			return false;
		}
		
		return true;
	}

	private static IChordRemote lookupWithRetry(IChordRemote source, IKey key) throws RemoteException {
		
		while (true) {
			try {
				return source.lookup(key).getRemote();
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
}
