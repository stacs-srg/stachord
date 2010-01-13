package uk.ac.standrews.cs.stachordRMI.util;

import java.rmi.RemoteException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;

/**
 * Class which provides the functionality to wait for a Chord ring to become complete - i.e. to fully stabilize. 
 * By calling waitForStableMethod with the complete set of nodes in the ring.
 *
 * @author Angus Macdonald (angus@cs.st-andrews.ac.uk)
 */
public class RingStabilizer {

	/**
	 * Wait for the set of nodes in the ring to stabilize. The {@link #uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode.stabilize()} operation will be called on each
	 * node until this happens.
	 * @param nodes_in_key_order All of the nodes in the chord ring sorted in key order.
	 */
	public static void waitForStableNetwork(SortedSet<IChordNode> nodes_in_key_order) {
		while (!isRingStable(nodes_in_key_order)) {
			for (IChordNode n:nodes_in_key_order){
				n.stabilize();
			}
		}
	}
	
	/**
	 * Sort a set of nodes by key value.
	 * 
	 * <p>Sets must be sorted before being used in the {@link #waitForStableNetwork(Set)} method.
	 * @param unsorted_node_set	A set of nodes which has not been sorted by key value.
	 * @return	Sorted set by key value.
	 */
	public static SortedSet<IChordNode> sortNodeSet(Set<IChordNode> unsorted_node_set){
		SortedSet<IChordNode> nodes_in_key_order = new TreeSet<IChordNode>(new NodeComparator());  //comparator implemented at the bottom of this class.
		
		nodes_in_key_order.addAll(unsorted_node_set);
		
		return nodes_in_key_order;
		
	}

	/**
	 * Tests whether all nodes in the ring have correct predecessor and successor links and that none of the nodes in the ring have failed.
	 * 
	 * @return true if the ring is stable
	 */
	private static boolean isRingStable(Set<IChordNode> nodes_in_key_order) {
		try {
			for (int i=0; i<nodes_in_key_order.size();i++){
				IChordNode nodes[]=nodes_in_key_order.toArray(new IChordNode[]{});

				IChordNode node = nodes[i];

				IChordRemote current = node.getProxy().getRemote();

				if (current.getPredecessor() == null) return false;

				IChordRemote nodePred=current.getPredecessor().getRemote();
				IChordRemote nodeSucc=current.getSuccessor().getRemote();


				if(!checkNodeStable(current)){
					return false;
				}


				// Check that the node's predecessor pointer refers to the same node as the precedding node in the key-ordered node list.
				if (i > 0) {
					if (!nodes[i - 1].getAddress().equals(nodePred.getAddress())){
						return false;
					}
				} else {

					if (!nodes[nodes.length - 1].getAddress().equals(nodePred.getAddress())){
						return false;
					}

				}

				// Check that the node's successor pointer refers to the same node as the next node in the key-ordered node list.
				if (i < nodes.length - 1) {
					if (!nodes[i + 1].getAddress().equals(nodeSucc.getAddress())){
						return false;
					}
				} else {
					if (!nodes[0].getAddress().equals(nodeSucc.getAddress())){
						return false;
					}
				}
			}

		} catch (RemoteException e) {
			return false;
		}

		return true;
	}


	private static boolean checkNodeStable(IChordRemote node) {

		return checkSuccessorStable(node) && checkPredecessorStable(node);
	}

	private static boolean checkSuccessorStable(IChordRemote node) {

		try {
			IChordRemote successor = node.getSuccessor().getRemote();

			return successor != null && successor.getPredecessor().getKey().equals(node.getKey());
		}

		catch (Exception e) {

			Diagnostic.trace(DiagnosticLevel.RUN, "error calling getPredecessor on successor");
			return false;
		}
	}

	private static boolean checkPredecessorStable(IChordRemote node) {

		try {
			IChordRemote predecessor = node.getPredecessor().getRemote();

			return predecessor != null && predecessor.getSuccessor().getKey().equals(node.getKey()); }

		catch (Exception e) {

			ErrorHandling.exceptionError(e, "error calling getSuccessor on predecessor");
			return false;
		}
	}

}