/*
 *  StAChord Library
 *  Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 *  http://asa.cs.st-andrews.ac.uk/
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.stachordRMI.test.stuff;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.impl.P2PNodeComparator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachord.impl.SuccessorList;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.testharness.interfaces.IChordNetworkNodeHandle;
import uk.ac.standrews.cs.stachord.testharness.interfaces.IChordNetwork;
import uk.ac.standrews.cs.stachord.testharness.interfaces.ISingleJVMChordNode;

public class Util {

	/**
	 * Tests whether all nodes in the ring have correct predecessor and successor links and that none of the nodes in the ring have failed.
	 * @param nodes an array of nodes, in ascending key order.
	 * @return
	 * @throws Exception
	 */
	public static boolean isChordRingStable(List<IChordRemote> nodeList) {

		IChordRemote nodes[]=sortNodes(nodeList);
		for (int i=0; i<nodes.length;i++){
			IChordRemote node = nodes[i];
			IChordRemote nodePred=node.getPredecessor();
			IChordRemote nodeSucc=node.getSuccessor();

			if(nodePred==null)return false;

			if (!node.isSimulatingFailure()) {
				// If the successor or predecessor have failed then the ring is not stable
				if(nodePred.isSimulatingFailure()||nodeSucc.isSimulatingFailure())
					return false;

				if(!checkNodeStable(node))return false;

				// Check that the node's predecessor pointer refers to the same node as the preceding node in the key-ordered node list.
				if (i > 0) {
					if (!nodes[i - 1].equals(nodePred))
						return false;
				} else
					if (!nodes[nodes.length - 1].equals(nodePred))
						return false;

				// Check that the node's successor pointer refers to the same node as the next node in the key-ordered node list.
				if (i < nodes.length - 1) {
					if (!nodes[i + 1].equals(nodeSucc))
						return false;
				} else
					if (!nodes[0].equals(nodeSucc))
						return false;
			}
		}
		return true;
	}

	public static void displayRing(List<IChordNetworkNodeHandle<?>> nodeHandles) throws Exception {

		TreeSet<IChordRemote> sortedNodes= new TreeSet<IChordRemote>();
		for(IChordNetworkNodeHandle<?> handle : nodeHandles){
			IChordRemote node = handle.getChordNode();
			sortedNodes.add(node);
		}

		for(IChordRemote node : sortedNodes){
			IChordRemote nodePred=node.getPredecessor();
			IChordRemote nodeSucc=node.getSuccessor();
			IKey predKey = nodePred==null?null:nodePred.getKey();
			IKey succKey = nodeSucc.getKey();
			System.out.println(predKey + " <-(pred)- " + node.getKey() + " -(succ)-> " + succKey);
		}
	}

	private static IChordRemote[] sortNodes(List<IChordRemote> nodeList) {
		Set<IChordRemote>nodes_in_key_order = new TreeSet<IChordRemote>(new P2PNodeComparator()); 	// use this to store nodes in key order
		for(IChordRemote n:nodeList) {
			nodes_in_key_order.add(n);
		}
		return nodes_in_key_order.toArray(new IChordRemote[]{});
	}

	public static boolean checkNodeStable(IChordRemote node) {

		return checkSuccessorStable(node) && checkPredecessorStable(node);
	}

	private static boolean checkSuccessorStable(IChordRemote node) {

		try {
			IChordRemote successor = node.getSuccessor();

			return successor != null && successor.getPredecessor() == node;
		}

		catch (Exception e) {

			Diagnostic.trace(DiagnosticLevel.RUN, "error calling getPredecessor on successor");
			return false;
		}
	}

	private static boolean checkPredecessorStable(IChordRemote node) {

		try {
			IChordRemote predecessor = node.getPredecessor();

			return predecessor != null && predecessor.getSuccessor() == node; }

		catch (Exception e) {

			ErrorHandling.exceptionError(e, "error calling getSuccessor on predecessor");
			return false;
		}
	}

	/*
	 * Tests whether all nodes in the ring have a finger table of the expected size.
	 */
	public static boolean areFingerTablesConsistent(List<IChordRemote> nodeList) throws Exception {
		IChordRemote nodeArray[]=nodeList.toArray(new IChordRemote[]{});
		// For each node...
		for (int i = 0; i < nodeArray.length; i++)
			if (!nodeArray[i].isSimulatingFailure()) {

				IChordRemote previous_finger = null;

				// For each finger...
				for (IChordRemote finger : nodeArray[i].getFingerList()) {

					// Check that the finger is not this node.
					if (finger.equals(nodeArray[i])) return false;

					// Removed this check since a finger may point to the successor if a previous successor has
					// recently failed and the finger hasn't been fixed yet.

					//					if (nodeArray[i].getSuccessor().equals(finger))
					//						return false;

					// Check that the finger is further in ring distance than the previous finger.
					if (previous_finger != null) {

						if (!nodeArray[i].getKey().firstCloserInRingThanSecond(previous_finger.getKey(), finger.getKey())) return false;

						previous_finger = finger;
					}
				}
			}

		return true;
	}

	/*
	 * Tests whether all nodes in the ring have valid successor lists.
	 */
	public static boolean areSuccessorListsConsistent(List<IChordRemote> nodeList) throws Exception {
		IChordRemote nodes[]=sortNodes(nodeList);
		int listSize = Math.min(SuccessorList.MAX_SUCCESSOR_LIST_SIZE, nodes.length - 1);
		// Check the successor list of each node.
		for (int i = 0; i < nodes.length; i++){
			IChordRemote node = nodes[i];
			if (!node.isSimulatingFailure()) {

				List<IChordRemote> successor_list = node.getSuccessorList();

				// The length of the successor lists should be MIN(max_successor_list_length, number_of_nodes - 1),
				// assuming that enough stabilises have been carried out.
				if (successor_list.size()!=listSize)return false;

				// Check that the successors of node with n'th key are n+1, n+2, n+3 etc, allowing for wrap-around.
				int expected_successor_index = i + 1;

				for (int j = 0; j < successor_list.size(); j++) {

					// Allow for wrap-around.
					if (expected_successor_index >= nodes.length) {
						expected_successor_index = 0;
					}

					if (!nodes[expected_successor_index].equals(node.getSuccessorList().get(j))) return false;

					expected_successor_index++;
				}
			}
		}
		return true;
	}

	/**
	 *
	 * @param source the node from which the routing takes place
	 * @param expected_target the node to which the key should map
	 * @param network_size the number of nodes in the simulation
	 * @return true if routing from the source to the target works as expected
	 * @throws P2PNodeException
	 * @throws Exception
	 */
	public static boolean isRoutingCorrect(IChordRemote source, IChordRemote expected_target, int network_size) throws P2PNodeException {

		// Check that a slightly smaller key than the target's key routes to the node.
		if (!expected_target.equals(
				source.lookup(new Key(expected_target.getKey().keyValue().subtract(BigInteger.ONE))))) return false;

		// Check that the target's own key routes to the target.
		if (!expected_target.equals(source.lookup(expected_target.getKey()))) return false;

		// Check that a slightly larger key than the node's key doesn't route to the node,
		// except when there is only one node, when it should do.
		if (network_size > 1) {
			if (expected_target.equals(
					source.lookup(new Key(expected_target.getKey().keyValue().add(BigInteger.ONE))))) return false;
		} else if (!expected_target.equals(
				source.lookup(new Key(expected_target.getKey().keyValue().add(BigInteger.ONE))))) return false;

		return true;
	}

	/**
	 *
	 * @param node_increment
	 * @return
	 * @throws Exception
	 */
	public static boolean isRoutingCorrectForSample(List<IChordRemote> nodeList, int node_increment) throws Exception {
		IChordRemote nodeArray[]=nodeList.toArray(new IChordRemote[]{});
		for (int i = 0; i < nodeArray.length; i += node_increment)
			if (!nodeArray[i].isSimulatingFailure()) {
				for (int j = 0; j < nodeArray.length; j += node_increment)
					if (!nodeArray[j].isSimulatingFailure())
						if (!isRoutingCorrect(nodeArray[i], nodeArray[j],nodeList.size())) return false;
			}

		return true;
	}

	public static List<IChordRemote> networkNodeHandles2IChordRemote(IChordNetwork<?> net) {

		List<IChordRemote> chordNodes = new ArrayList<IChordRemote>();
		for(IChordNetworkNodeHandle<?> n:net.getNodes()) {
			chordNodes.add(n.getChordNode());
		}
		return chordNodes;
	}

	public static void formRing(List<IChordNetworkNodeHandle<ISingleJVMChordNode>> nodeHandles, IChordNetworkNodeHandle<ISingleJVMChordNode> knownHandle) {

		// Get the known node
		IChordNode known = knownHandle.getNode().getNode();

		known.createRing();

		// Ensure that the known node's predecessor pointer is correct (should point at itself)
		known.stabilize();

		for (IChordNetworkNodeHandle<ISingleJVMChordNode> handle : nodeHandles) {
			IChordNode node = handle.getNode().getNode();

			if (!node.equals(known)) {
				// why do I have to do this?
				node.setPredecessor(null);
				try {
					node.join(known);
				} catch (P2PNodeException e) {
					// TODO Auto-generated catch block
					ErrorHandling.hardExceptionError(e, "error occured while (re)joining the network");
				}
				IChordNode temp = (IChordNode) node.getSuccessor().getPredecessor();
				inHandleList(nodeHandles, temp);
				node.stabilize();
				temp.stabilize();
			}
		}
	}

	private static void inHandleList(List<IChordNetworkNodeHandle<ISingleJVMChordNode>> nodeHandles, IChordNode node) {
		for (IChordNetworkNodeHandle<ISingleJVMChordNode> nodeHandle : nodeHandles) {
			if (nodeHandle.getNode().getNode().equals(node))
				return;
		}
		ErrorHandling.hardError("node is not in list");
	}
}
