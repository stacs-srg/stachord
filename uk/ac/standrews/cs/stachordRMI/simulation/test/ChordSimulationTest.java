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
/*
 * Created on Dec 16, 2004 at 3:07:32 PM.
 */
package uk.ac.standrews.cs.stachordRMI.simulation.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.SortedSet;

import org.junit.BeforeClass;
import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.INodeFactory;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.fingerTableFactories.GeometricFingerTableFactory;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTableFactory;
import uk.ac.standrews.cs.stachordRMI.nodeFactories.ChordNodeFactory;
import uk.ac.standrews.cs.stachordRMI.simulation.ChordSimulation;


/**
 * Test class for Chord ring using simulation harness.
 *
 * @author graham
 */
public class ChordSimulationTest {

	// TODO get rid of this - superseded by stachord.test.SimulationTests

	/*
	 * Hide diagnostics for all tests.
	 */
	@BeforeClass
	public static void setUp() throws Exception {
		Diagnostic.setLevel(DiagnosticLevel.NONE);
	}

	/**
	 * Tests the structural properties of an empty ring.
	 */
	@Test
	public void testEmptyRing() {

		ChordSimulation sim = makeSimulation(0, 0, false);

		// Shouldn't be any nodes!
		assertTrue(sim.getNodes().size() == 0);
		assertTrue(sim.getNodeCount() == 0);
	}

	/**
	 * Tests the structural properties of a ring with one node.
	 * @throws Exception
	 */
	@Test
	public void testRing1Structure() throws Exception {

			// -----------------------------------------------------------
			// Make a ring with one node.

			ChordSimulation sim = makeSimulation(1, 0, false);

			// -----------------------------------------------------------
			// Check the number of nodes.

			SortedSet<IChordNode> nodes = sim.getNodesInKeyOrder();

			assertTrue(nodes.size() == 1);
			assertTrue(sim.getNodeCount() == 1);

			IChordNode first = nodes.first();
			// -----------------------------------------------------------
			// Check the node's key.

			IKey k0 = first.getKey();
			assertEquals("8334f9b826e9f5677ea0ce8579cd968d71882f1d", k0.toString());

			// -----------------------------------------------------------
			// Check the node's address.

			assertEquals("0.0.0.0", first.getAddress().getAddress().getHostAddress());
			assertEquals(0, first.getAddress().getPort());

			// -----------------------------------------------------------
			// The ring should be stable.

			assertTrue(sim.isRingStable());

			// -----------------------------------------------------------
			// Check successor lists.

			assertTrue(sim.areSuccessorListsConsistent());

			// -----------------------------------------------------------
			// The finger tables should be empty, because they haven't been set up yet.

			assertTrue(sim.areFingerTablesConsistent());


	}

	/**
	 * Tests the structural properties of a ring with two nodes.
	 * @throws Exception
	 */
	@Test
	public void testRing2Structure() throws Exception {


			// -----------------------------------------------------------
			// Make a ring with two nodes.

			ChordSimulation sim = makeSimulation(2, 0, false);

			// -----------------------------------------------------------
			// Check the number of nodes.

			List<IChordNode> nodes = sim.getNodes();

			assertTrue(sim.getNodes().size() == 2);
			assertTrue(sim.getNodeCount() == 2);

			// -----------------------------------------------------------
			// Check the nodes' keys.

			assertEquals("8334f9b826e9f5677ea0ce8579cd968d71882f1d", sim.getNodes().get(0).getKey().toString());

			assertEquals("c91fe91e43cde6d2ac423d11fd4c4b21e67c4d4c", sim.getNodes().get(1).getKey().toString());

			// -----------------------------------------------------------
			// Check the nodes' addresses.

			assertEquals("0.0.0.0", nodes.get(0).getAddress().getAddress().getHostAddress());
			assertEquals(0, nodes.get(0).getAddress().getPort());

			assertEquals("0.0.0.1", nodes.get(1).getAddress().getAddress().getHostAddress());
			assertEquals(1, nodes.get(1).getAddress().getPort());

			// -----------------------------------------------------------
			// The ring should be stable.

			assertTrue(sim.isRingStable());

			// -----------------------------------------------------------
			// Check successor lists.

			assertTrue(sim.areSuccessorListsConsistent());

			// -----------------------------------------------------------
			// The finger tables should be empty, because they haven't been set up yet.

			assertTrue(sim.areFingerTablesConsistent());


	}

	/**
	 * Tests the structural properties of a ring with three nodes.
	 * @throws Exception
	 */
	@Test
	public void testRing3Structure() throws Exception {


			// -----------------------------------------------------------
			// Make a ring with three nodes.

			ChordSimulation sim = makeSimulation(3, 0, false);

			// -----------------------------------------------------------
			// Check the number of nodes.

			//IChordNode[] nodes = getNodesInKeyOrder(chord_simulation);

			assertTrue(sim.getNodes().size() == 3);
			assertTrue(sim.getNodeCount() == 3);


			//Why do this twice.. wait a minute, did I do this?

			// -----------------------------------------------------------
			// The ring should be stable.

			assertTrue(sim.isRingStable());

			// -----------------------------------------------------------
			// Check successor lists.

			assertTrue(sim.areSuccessorListsConsistent());

			// -----------------------------------------------------------
			// Test finger tables.

			assertTrue(sim.areFingerTablesConsistent());


	}

	/**
	 * Tests the structural properties of rings with various numbers of nodes.
	 * @throws Exception
	 */
	@Test
	public void testRingStructure() throws Exception {

		// 5000/20/100/10 completed in about 8 hours.

		int MAX_NUMBER_OF_NODES = 100;
		int NODE_INCREMENT = 20;
		int MAX_NUMBER_OF_NEIGHBOURS = 0;
		int NEIGHBOUR_INCREMENT = 50;


			for (int number_of_nodes = 0; number_of_nodes <= MAX_NUMBER_OF_NODES; number_of_nodes += NODE_INCREMENT)
				for (int number_of_neighbours = 0; number_of_neighbours <= MAX_NUMBER_OF_NEIGHBOURS; number_of_neighbours += NEIGHBOUR_INCREMENT) {

					// -----------------------------------------------------------
					// Make a ring.

					ChordSimulation sim = makeSimulation(number_of_nodes, number_of_neighbours, false);

					Diagnostic.trace(DiagnosticLevel.RUN, "number of nodes: ", number_of_nodes);
					Diagnostic.trace(DiagnosticLevel.RUN, "number of neighbours: ", number_of_neighbours);

					// -----------------------------------------------------------
					// Check the number of nodes.

					//IChordNode[] nodes = getNodesInKeyOrder(chord_simulation);

					assertTrue(sim.getNodes().size() == number_of_nodes);
					assertTrue(sim.getNodeCount() == number_of_nodes);

					// -----------------------------------------------------------
					// The ring should be stable.

					assertTrue(sim.isRingStable());

					// -----------------------------------------------------------
					// Check successor lists.

					assertTrue(sim.areSuccessorListsConsistent());

					// -----------------------------------------------------------
					// Test finger tables.

					assertTrue(sim.areFingerTablesConsistent());
				}

	}

	/**
	 * Tests the structural properties of rings with various numbers of nodes.
	 * @throws Exception
	 */
	@Test
	public void testRingStructureRandomised() throws Exception {

		int MAX_NUMBER_OF_NODES = 12;
		int NODE_INCREMENT = 1;
		int MAX_NUMBER_OF_NEIGHBOURS = 0;
		int NEIGHBOUR_INCREMENT = 100;


			for (int number_of_nodes = 0; number_of_nodes <= MAX_NUMBER_OF_NODES; number_of_nodes += NODE_INCREMENT)
				for (int number_of_neighbours = 0; number_of_neighbours <= MAX_NUMBER_OF_NEIGHBOURS; number_of_neighbours += NEIGHBOUR_INCREMENT) {

					// -----------------------------------------------------------
					// Make a ring.

					ChordSimulation sim = makeSimulation(number_of_nodes, number_of_neighbours, true);

					// -----------------------------------------------------------
					// Check the number of nodes.

					//IChordNode[] nodes = chord_simulation.getNodesInKeyOrder();

					assertTrue(sim.getNodes().size() == number_of_nodes);
					assertTrue(sim.getNodeCount() == number_of_nodes);

					// -----------------------------------------------------------
					// The ring should be stable.

					assertTrue(sim.isRingStable());

					// -----------------------------------------------------------
					// Check successor lists.

					assertTrue(sim.areSuccessorListsConsistent());

					// -----------------------------------------------------------
					// Test finger tables.

					assertTrue(sim.areFingerTablesConsistent());
				}

	}

	/**
	 * Tests the routing properties of a ring with various numbers of nodes.
	 * @throws Exception
	 */
	@Test
	public void testRingRouting() throws Exception {

		int MAX_NUMBER_OF_NODES = 100;
		int NODE_INCREMENT = 20;
		int MAX_NUMBER_OF_NEIGHBOURS = 0;
		int NEIGHBOUR_INCREMENT = 5;

		for (int number_of_nodes = 0; number_of_nodes <= MAX_NUMBER_OF_NODES; number_of_nodes += NODE_INCREMENT)
			for (int number_of_neighbours = 0; number_of_neighbours <= MAX_NUMBER_OF_NEIGHBOURS; number_of_neighbours += NEIGHBOUR_INCREMENT) {

				ChordSimulation sim = makeSimulation(number_of_nodes, number_of_neighbours, false);


					assertTrue(sim.isRoutingCorrectForSample(1));


			}
	}

	/**
	 * Tests the routing properties of a ring with various numbers of nodes.
	 * @throws Exception
	 */
	@Test
	public void testRingRoutingRandomised() throws Exception {

		int MAX_NUMBER_OF_NODES = 100;
		int NODE_INCREMENT = 20;
		int MAX_NUMBER_OF_NEIGHBOURS = 0;
		int NEIGHBOUR_INCREMENT = 5;

		for (int number_of_nodes = 0; number_of_nodes <= MAX_NUMBER_OF_NODES; number_of_nodes += NODE_INCREMENT)
			for (int number_of_neighbours = 0; number_of_neighbours <= MAX_NUMBER_OF_NEIGHBOURS; number_of_neighbours += NEIGHBOUR_INCREMENT) {

				ChordSimulation sim = makeSimulation(number_of_nodes, number_of_neighbours, true);


					assertTrue(sim.isRoutingCorrectForSample(NODE_INCREMENT));


			}
	}

	/**
	 * Tests the structural properties of rings with various numbers of nodes.
	 * @throws Exception
	 */
	@Test
	public void testRingRecovery() throws Exception {

		int MAX_NUMBER_OF_NODES = 10;
		int NODE_INCREMENT = 3;
		int MAX_NUMBER_OF_NEIGHBOURS = 0;
		int NEIGHBOUR_INCREMENT = 10;

//		int DELAY = 1000;

		for (int number_of_nodes = 0; number_of_nodes <= MAX_NUMBER_OF_NODES; number_of_nodes += NODE_INCREMENT)
			for (int number_of_neighbours = 0; number_of_neighbours <= MAX_NUMBER_OF_NEIGHBOURS; number_of_neighbours += NEIGHBOUR_INCREMENT) {

				// -----------------------------------------------------------
				// Make a ring.

				ChordSimulation sim = makeSimulation(number_of_nodes, number_of_neighbours, false);
//				chord_simulation.runAll();

				// -----------------------------------------------------------
				// Check the number of nodes.

				//IChordNode[] nodes = getNodesInKeyOrder(chord_simulation);

				assertTrue(sim.getNodes().size() == number_of_nodes);
				assertTrue(sim.getNodeCount() == number_of_nodes);

				// -----------------------------------------------------------
				// Check initial stability.

				//try {
				assertTrue("Ring is not stable before node is removed",sim.isRingStable());
				//} catch (Exception e) { fail(); }

				// -----------------------------------------------------------
				// The ring should eventually become stable after one node fails.

				if (sim.getNodes().size() > 0)
					//nodes[0].setFailed(true);
					sim.removeNodeFromRing(sim.getNodes().get(0));

				//try {
				assertTrue("Ring is not stable after node was removed", sim.isRingStable());
				//} catch (Exception e) { fail(); }

//				boolean ring_stable = false;
//
//				while (!ring_stable) {
//
//					try {
//						ring_stable = isRingStable(nodes);
//					}
//					catch (Exception e) { /* Ignore */ }
//
//					try {
//						Thread.sleep(DELAY);
//					}
//					catch (InterruptedException e) { /* Ignore */ }
//				}

				// -----------------------------------------------------------
				// Check successor lists.

				//try {
				assertTrue(sim.areSuccessorListsConsistent());

				//} catch (Exception e) { fail(); }

				// -----------------------------------------------------------
				// Test finger tables.

				//try {
				assertTrue(sim.areFingerTablesConsistent());

				//} catch (Exception e) { fail(); }
			}
	}

//	/**
//	 * Tests whether all nodes in the ring have correct predecessor and successor links and that none of the nodes in the ring have failed.
//	 * @param nodes an array of nodes, in ascending key order.
//	 * @return
//	 * @throws Exception
//	 */
//	private boolean isRingStable(ChordSimulation chord_simulation) throws Exception {
//		for (int i=0; i<chord_simulation.getNodes().size();i++){
//
//			SortedSet<IChordNode> sorted = chord_simulation.getNodesInKeyOrder();
//
//			IChordNode nodes[]=sorted.toArray(new IChordNode[]{});
//			IChordNode node = nodes[i];
//			IChordRemote nodePred=node.getPredecessor();
//			IChordRemote nodeSucc=node.getSuccessor();
//
//			if (!node.isSimulatingFailure()) {
//				// If the successor or predecessor have failed then the ring is not stable
//				if(nodePred.isSimulatingFailure()||nodeSucc.isSimulatingFailure()){
//					return false;
//				}
//
//				// Check that the node's predecessor pointer refers to the same node as the precedding node in the key-ordered node list.
//				if (i > 0) {
//					if (!nodes[i - 1].equals(nodePred))
//						return false;
//				} else
//					if (!nodes[nodes.length - 1].equals(nodePred))
//						return false;
//
//				// Check that the node's successor pointer refers to the same node as the next node in the key-ordered node list.
//				if (i < nodes.length - 1) {
//					if (!nodes[i + 1].equals(nodeSucc))
//						return false;
//				} else
//					if (!nodes[0].equals(nodeSucc))
//						return false;
//			}
//		}
//		return true;
//	}

//	/*
//	 * Tests whether all nodes in the ring have a finger table of the expected size.
//	 */
//	private boolean areFingerTablesConsistent(List<IChordNode> nodelist) throws Exception {
//		IChordNode nodes[]=nodelist.toArray(new IChordNode[]{});
//		// For each node...
//		for (int i = 0; i < nodes.length; i++)
//			if (!nodes[i].isSimulatingFailure()) {
//
//				IChordRemote previous_finger = null;
//
//				// For each finger...
//				for (RingSegment seg : nodes[i].getFingerTable()) {
//
//					IChordRemote finger = seg.finger;
//
//					// Check that the finger is not this node.
//					if (finger.equals(nodes[i])) return false;
//
//					// Removed this check since a finger may point to the successor if a previous successor has recently failed and the finger hasn't been fixed yet.
//					// if (nodes[i].getSuccessor().equals(finger)) return false;
//
//					// Check that the finger is further in ring distance than the previous finger.
//					if (previous_finger != null) {
//
//						if (!nodes[i].getKey().firstCloserInRingThanSecond(previous_finger.getKey(), finger.getKey())) return false;
//
//						previous_finger = finger;
//					}
//				}
//			}
//
//		return true;
//	}

//	/*
//	 * Tests whether all nodes in the ring have valid successor lists.
//	 */
//	private boolean areSuccessorListsConsistent(ChordSimulation chord_simulation) throws Exception {
//		Set<IChordNode> sorted = chord_simulation.getNodesInKeyOrder();
//		IChordNode nodes[]=sorted.toArray(new IChordNode[]{});
//
//		int listSize = Math.min(SuccessorList.MAX_SIZE, nodes.length - 1);
//		// Check the successor list of each node.
//		for (int i = 0; i < nodes.length; i++){
//			IChordNode node = nodes[i];
//			if (!node.isSimulatingFailure()) {
//
//				List<IChordRemote> successor_list = node.getSuccessorList();
//
//				// The length of the successor lists should be MIN(max_successor_list_length, number_of_nodes - 1),
//				// assuming that enough stabilises have been carried out.
//				if (successor_list.size()!=listSize)return false;
//
//				// Check that the successors of node with n'th key are n+1, n+2, n+3 etc, allowing for wrap-around.
//				int expected_successor_index = i + 1;
//
//				for (int j = 0; j < successor_list.size(); j++) {
//
//					// Allow for wrap-around.
//					if (expected_successor_index >= nodes.length)
//						expected_successor_index = 0;
//
//					if (!nodes[expected_successor_index].equals(node.getSuccessorList().get(j))) return false;
//
//					expected_successor_index++;
//				}
//			}
//		}
//		return true;
//	}
//
//	/**
//	 * @param source the node from which the routing takes place
//	 * @param expected_target the node to which the key should map
//	 * @param number_of_nodes the number of nodes in the simulation
//	 * @return true if routing from the source to the target works as expected
//	 * @throws Exception
//	 */
//	private boolean isRoutingCorrect(IChordNode source, IChordNode expected_target, int number_of_nodes) throws Exception {
//
//		// Check that a slightly smaller key than the target's key routes to the node.
//		if (!expected_target.equals(
//				source.findSuccessor(new Key(expected_target.getKey().keyValue().subtract(BigInteger.ONE))))) return false;
//
//		// Check that the target's own key routes to the target.
//		if (!expected_target.equals(source.findSuccessor(expected_target.getKey()))) return false;
//
//		// Check that a slightly larger key than the node's key doesn't route to the node,
//		// except when there is only one node, when it should do.
//		if (number_of_nodes > 1) {
//			if (expected_target.equals(
//					source.findSuccessor(new Key(expected_target.getKey().keyValue().add(BigInteger.ONE))))) return false;
//		} else if (!expected_target.equals(
//				source.findSuccessor(new Key(expected_target.getKey().keyValue().add(BigInteger.ONE))))) return false;
//
//		return true;
//	}
//
//	/*
//	 * Tests routing between all pairs in the given node array.
//	 */
//	private boolean testRoutingCombinations(SortedSet<IChordNode> nodeset, int node_increment) throws Exception {
//		IChordNode nodes[]=nodeset.toArray(new IChordNode[]{});
//		for (int i = 0; i < nodes.length; i += node_increment)
//			if (!nodes[i].isSimulatingFailure())
//				for (int j = 0; j < nodes.length; j += node_increment)
//					if (!nodes[j].isSimulatingFailure())
//						if (!isRoutingCorrect(nodes[i], nodes[j], nodes.length)) return false;
//
//		return true;
//	}

//	/**
//	 * @param chord_simulation the simulation instance
//	 * @return the nodes in the simulation, in increasing key order
//	 */
//	private SortedSet<IP2PNode> getNodesInKeyOrder(ChordSimulation chord_simulation) {
//
//		chord_simulation.getNodesInKeyOrder();
//		IChordNode[] sorted_nodes = new IChordNode[ts.size()];
//
//		int count = 0;
//
//		for (IP2PNode node : ts) sorted_nodes[count++] = (IChordNode)node;
//
//		return sorted_nodes;
//	}

	private ChordSimulation makeSimulation(int number_of_nodes, int number_of_neighbours, boolean randomiseJoinPosition) {

		IFingerTableFactory ftf=new GeometricFingerTableFactory();
		INodeFactory<IChordNode> nf = new ChordNodeFactory(ftf);

		ChordSimulation sim = new ChordSimulation(number_of_nodes,number_of_neighbours, nf);
		sim.initialiseP2PLinks();

//		try {
//			chord_simulation.formRing();
//			//chord_simulation.stabiliseUntilQuiescent();
//			//chord_simulation.associateNeighbours();
//			chord_simulation.populateFingerTables();
//
//		} catch (Exception e) { fail("ring structure test failed: " + number_of_nodes + " nodes, " + number_of_neighbours + " neighbours. " + e.getMessage()); return null; }
		return sim;
	}
}