package uk.ac.standrews.cs.stachordRMI.test.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.SortedSet;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
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
public class RingIntegrityLogic {

	/**
	 * Wait for the set of nodes in the ring to stabilize. The {@link #uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode.stabilize()} operation will be called on each
	 * node until this happens.
	 * @param nodes All of the nodes in the chord ring sorted in key order.
	 */
	public static void waitForStableNetwork(SortedSet<IChordRemote> nodes) {
		
		while (!isRingStable(nodes)) {

			try { Thread.sleep(5000); }
			catch (InterruptedException e) {}
		}
		Diagnostic.trace(DiagnosticLevel.RUN, "Stabilized ring.");
	}

	/**
	 * Tests whether all nodes in the ring have correct predecessor and successor links and that none of the nodes in the ring have failed.
	 * 
	 * @return true if the ring is stable
	 */
	private static boolean isRingStable(SortedSet<IChordRemote> nodes) {
		try {
			IChordRemote[] node_array = nodes.toArray(new IChordRemote[]{});

			for (int i=0; i < nodes.size(); i++) {

				IChordRemote current = node_array[i];

				if (current.getPredecessor() == null) return false;

				IChordRemote predecessor = current.getPredecessor().getRemote();
				IChordRemote successor =   current.getSuccessor().getRemote();

				if (!isNodeStable(current)) return false;

				// Check that the node's predecessor pointer refers to the same node as the preceding node in the key-ordered node list.
				if (i > 0) {
					if (!node_array[i - 1].getAddress().equals(predecessor.getAddress())) return false;
				} else {
					if (!node_array[node_array.length - 1].getAddress().equals(predecessor.getAddress())) return false;
				}

				// Check that the node's successor pointer refers to the same node as the next node in the key-ordered node list.
				if (i < node_array.length - 1) {
					if (!node_array[i + 1].getAddress().equals(successor.getAddress())) return false;
				} else {
					if (!node_array[0].getAddress().equals(successor.getAddress())) return false;
				}
			}

		} catch (RemoteException e) {
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
	
	public static void checkFingersConsistent(SortedSet<IChordRemote> nodes) throws RemoteException {

		for (IChordRemote node : nodes) {

			IChordRemote previous_finger = null;

			// For each finger...
			for (IChordRemoteReference finger_reference : node.getFingerList()) {
				
				IChordRemote finger = finger_reference.getRemote();
				
				// Check that the finger is not this node.
				assertNotSame(finger.getKey(), node.getKey());

				// Check that the finger is closer in ring distance than the previous finger,
				// since the finger table is ordered with farthest finger first.
				assertFalse(previous_finger != null && !node.getKey().firstCloserInRingThanSecond(finger.getKey(), previous_finger.getKey()));
				
				previous_finger = finger;
			}
		}
	}
	
	public static void checkSuccessorsConsistent(SortedSet<IChordRemote> nodes) throws P2PNodeException, IOException {
		
		IChordRemote[] node_array = nodes.toArray(new IChordRemote[]{});
		
		// Check the successor list of each node.
		for (int i = 0; i < nodes.size(); i++) {
			
			IChordRemote node = node_array[i];

			List<IChordRemoteReference> successor_list = node.getSuccessorList();

			// The max length of the successor lists should be MIN(max_successor_list_length, number_of_nodes - 1).
			assertTrue(successor_list.size() <= Math.min(SuccessorList.MAX_SUCCESSOR_LIST_SIZE, nodes.size() - 1));

			// Check that the successors of node with n'th key are n+1, n+2, n+3 etc, allowing for wrap-around.
			int expected_successor_index = i + 1;

			for (int j = 0; j < successor_list.size(); j++) {

				// Allow for wrap-around.
				if (expected_successor_index >= node_array.length) {
					expected_successor_index = 0;
				}

				assertEquals(node_array[expected_successor_index].getKey(), successor_list.get(j).getRemote().getKey());

				expected_successor_index++;
			}
		}
	}
}
