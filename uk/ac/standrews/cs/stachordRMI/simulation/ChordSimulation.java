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
 * Created on 03-Dec-2004
 */
package uk.ac.standrews.cs.stachordRMI.simulation;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.INodeFactory;
import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PNode;
import uk.ac.standrews.cs.nds.p2p.simulation.impl.Route;
import uk.ac.standrews.cs.nds.p2p.simulation.impl.SimulationFramework;
import uk.ac.standrews.cs.nds.p2p.simulation.interfaces.IP2PSimulation;
import uk.ac.standrews.cs.nds.p2p.simulation.util.SimProgressWindow;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.Pair;
import uk.ac.standrews.cs.stachordRMI.impl.NextHopResultStatus;
import uk.ac.standrews.cs.stachordRMI.impl.SuccessorList;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTable;
import uk.ac.standrews.cs.stachordRMI.testharness.logic.Util;

/**
 * @author stuart, al, graham
 */
public class ChordSimulation extends SimulationFramework<IChordNode> implements IP2PSimulation<IChordNode> {

	public static int keylen = Key.KEY_LENGTH;
	//dead nodes
	private final List<IChordNode> rip;

	private final int neighbourhood_size;		// Size of neighbourhood - TODO should this be here - al

	public ChordSimulation(int node_count, int neighbourhood_size, INodeFactory<IChordNode> nfactory, boolean showProgress){
		super(node_count,nfactory, showProgress);
		this.neighbourhood_size = Math.min(neighbourhood_size, node_count);
		rip=new ArrayList<IChordNode>();
	}

	public ChordSimulation(int node_count, int neighbourhood_size, INodeFactory<IChordNode> nfactory){
		this(node_count,neighbourhood_size,nfactory,false);
	}

	public ChordSimulation(String[] keyStrings, int neighbourhood_size, INodeFactory<IChordNode> nf) {
		super(keyStrings,nf, false);
		this.neighbourhood_size = Math.min(neighbourhood_size, keyStrings.length);
		rip=new ArrayList<IChordNode>();
	}

	public void initialiseP2PLinks() {
		try {
			Diagnostic.trace(DiagnosticLevel.RUN, "Forming Chord ring");
			formRing();

			Diagnostic.trace(DiagnosticLevel.RUN, "Checking stability");
			if (!isRingStable()) {
				ErrorHandling.hardError("ring is not stable");
			}

			Diagnostic.trace(DiagnosticLevel.RUN, "Building successor lists");
			populateSuccessorLists();

			Diagnostic.trace(DiagnosticLevel.RUN, "Associating neighbours");
			associateNeighbours();

			Diagnostic.trace(DiagnosticLevel.RUN, "Ring is stable, now populating finger tables");
			populateFingerTables();

			Diagnostic.trace(DiagnosticLevel.RUN, "Finger tables populated");
			Diagnostic.trace(DiagnosticLevel.RUN, "Simulation initialisation complete");

		} catch (Exception e) {
			ErrorHandling.exceptionError(e, "Instantiation exception");
		}
	}

	private void populateSuccessorLists() {
		int listLength=Math.min(SuccessorList.MAX_SUCCESSOR_LIST_SIZE, nodes.size()-1);
		for(int i=0;i<listLength;i++) {
			for(IP2PNode n:nodes){
				IChordNode node=(IChordNode)n;
				if(!node.isSimulatingFailure()) {
					node.stabilize();
				}
			}
		}
	}

	/**
	 * This method returns the route from a given node searching for a given key
	 *
	 * @param node the node from which we are searching
	 * @param k the hey to be found starting at node node
	 * @return the route from node to the node responsible for key k
	 */
	public Route<IChordNode> makeRoute(IChordNode node, IKey k ) {

		Pair<NextHopResultStatus, IChordRemote> result = null;
		IChordNode next = node;
		boolean notFound = true;
		Route<IChordNode> r = new Route<IChordNode>( node,k );

		while (notFound) {
			try {
				result = next.nextHop(k);
				switch (result.first) {

					case NEXT_HOP: {
						next = (IChordNode)result.second;
						r.addHop(next);
						break;
					}

					case FINAL: {
						next = (IChordNode)result.second;
						r.addHop(next);
						notFound = false;
						break;
					}

					default: {
						ErrorHandling.hardError("nextHop call returned NextHopResult with unrecognised code");
					}
				}
			} catch (Exception e) {
				ErrorHandling.hardError("ErrorHandling calling nextHop on closest preceding node");
			}
		}

		return r;
	}

	/**
	 * Forms the Chord ring by making appropriate join and stabilise calls.
	 *
	 * @param randomiseJoinPosition true if nodes should be joined at random positions in the ring (takes longer)
	 * @throws Exception
	 */
	public void formRing() throws Exception {

		if (nodes.size() > 0) {
			int count = 0; // for progress reporting

			IChordNode known = nodes.get(0);
			createSingleNodeNetwork(known);

			for (int i = 1; i < nodes.size(); i++) {
				IChordNode node = nodes.get(i);
				addNodeToRing(node, known);
				showProgress(count++, progress_granularity, linebreak_granularity, DiagnosticLevel.RUN);
			}
		}
	}

	private void createSingleNodeNetwork(IChordNode node) throws P2PNodeException {
		node.createRing();
		node.stabilize();
	}

	private void addNodeToRing(IChordNode node, IChordNode known) throws P2PNodeException {
		//set node's successor
		node.join(known);
		//remember node's successor's predecessor
		IChordNode oldPred;
		try {
			oldPred = (IChordNode)node.getSuccessor().getPredecessor();
		} catch (RemoteException e) {
			throw new P2PNodeException(null); // TODO AL should put a parameter in here
		}
		//stabilise node to update node's successor's predecessor
		node.stabilize();
		//stabilse remembered node to update its successor and to set node's predessor
		oldPred.stabilize();
	}

	private void addNodeToRing(IChordNode node) throws P2PNodeException {
		if(nodes.size()==0) {
			createSingleNodeNetwork(node);
		} else {
			addNodeToRing(node, nodes.get(0));
		}
	}

	public void addNodeToRing() throws P2PNodeException {
		addNodeToRing(addNewNode(null));
	}

	public void addNodeToRing(String k) throws P2PNodeException {
		addNodeToRing(addNewNode(k));
	}

	/**
	 * Removes the specifed node from the network of which it is currently a member. The network must be already be stable
	 * to ensure that the network is stable after removal of the specified node.
	 *
	 * @param node
	 */
	public void removeNodeFromRing(IChordNode node) {
		if(removeNode(node)){
			IChordNode pred = null;
			try {
				pred = (IChordNode) node.getPredecessor();
			} catch (RemoteException e) {
				// TODO if pred has failed what do we do?
			}
			IChordNode succ = null;
			try {
				succ = (IChordNode) node.getSuccessor();
			} catch (RemoteException e) {
				// TODO similarly with successor failure.
			}
			node.setSimulatingFailure(true);
			rip.add(node);
			if( succ != null ) {
				succ.checkPredecessor();
			}
			/*
			 * It is necessary to stabilise 'pred' twice. Failure of 'node' is detected by 'pred' on the call to
			 * 'node.getPredecessor()'. However failure can be detected at any point where a 'stabilise' makes a call on
			 * 'node'. Following failure detection a call to 'findWorkingSucessor()' is made to update the successor
			 * reference appropriately. 'stabilise' then returns. Thus a single call to 'stabilise' in the precesence of a
			 * failed successor means that 'pred' will not call 'notify' on its new sucessor.
			 */
			// Detect failure of 'node' and update successor pointer.
			pred.stabilize();
			// Notify new successor and retrieve its successor list.
			pred.stabilize();

			// Ensure that all the successor lists are up-to-date
			populateSuccessorLists();
		}
	}

	/**
	 * Tests whether all nodes in the ring have correct predecessor and successor links and that none of the nodes in the ring have failed.
	 * 
	 * @return true if the ring is stable
	 */
	public boolean isRingStable() {

		for (int i=0; i<nodes.size();i++){
			IChordNode nodes[]=getNodesInKeyOrder().toArray(new IChordNode[]{});
			IChordNode node = nodes[i];
			IChordRemote nodePred;
			IChordRemote nodeSucc;
			try {
				nodePred = node.getPredecessor();
				nodeSucc=node.getSuccessor();
			} catch (RemoteException e) {
				return false;
			}
			

			if (!node.isSimulatingFailure()) {
				// If the successor or predecessor have failed then the ring is not stable
				if(nodePred.isSimulatingFailure()||nodeSucc.isSimulatingFailure())
					return false;

				if(!checkNodeStable(node))return false;

				// Check that the node's predecessor pointer refers to the same node as the precedding node in the key-ordered node list.
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

	/*
	 * Tests whether all nodes in the ring have a finger table of the expected size.
	 */
	public boolean areFingerTablesConsistent() throws Exception {

		IChordNode nodeArray[] = nodes.toArray(new IChordNode[]{});

		// For each node...
		for (int i = 0; i < nodeArray.length; i++)
			if (!nodeArray[i].isSimulatingFailure()) {

				IChordRemote previous_finger = null;

				// For each finger...
				for (IChordRemote finger : nodeArray[i].getFingerTable().getFingers()) {


					// Check that the finger is not this node.
					if (finger.equals(nodeArray[i])) return false;

					// Removed this check since a finger may point to the successor if a previous successor has
					// recently failed and the finger hasn't been fixed yet.

					// Check replaced by stuart. Suitable calls will have been made to update the finger tables. This
					// method needs to check if the update has worked as expected.
					if (nodeArray[i].getSuccessor().equals(finger))
						return false;

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
	public boolean areSuccessorListsConsistent() throws Exception {
		IChordNode nodes[]=getNodesInKeyOrder().toArray(new IChordNode[]{});

		int listSize = Math.min(SuccessorList.MAX_SUCCESSOR_LIST_SIZE, nodes.length - 1);
		// Check the successor list of each node.
		for (int i = 0; i < nodes.length; i++){
			IChordNode node = nodes[i];
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
	 * @return true if routing from the source to the target works as expected
	 * @throws Exception
	 */
	public boolean isRoutingCorrect(IChordRemote source, IChordRemote expected_target) throws P2PNodeException {

		return Util.isRoutingCorrect(source, expected_target, nodes.size());
	}

	public boolean isRoutingCorrectForSample(int node_increment) throws Exception {
		IChordNode nodeArray[]=nodes.toArray(new IChordNode[]{});
		for (int i = 0; i < nodeArray.length; i += node_increment)
			if (!nodeArray[i].isSimulatingFailure()) {
				for (int j = 0; j < nodeArray.length; j += node_increment)
					if (!nodeArray[j].isSimulatingFailure())
						if (!isRoutingCorrect(nodeArray[i], nodeArray[j])) return false;
			}

		return true;
	}

	public boolean checkNodeStable(IChordRemote node) {

		return checkSuccessorStable(node) && checkPredecessorStable(node);
	}

	private boolean checkSuccessorStable(IChordRemote node) {

		try {
			IChordRemote successor = node.getSuccessor();

			return successor != null && successor.getPredecessor() == node;
		}

		catch (Exception e) {

			Diagnostic.trace(DiagnosticLevel.RUN, "error calling getPredecessor on successor");
			return false;
		}
	}

	private boolean checkPredecessorStable(IChordRemote node) {

		try {
			IChordRemote predecessor = node.getPredecessor();

			return predecessor != null && predecessor.getSuccessor() == node; }

		catch (Exception e) {

			ErrorHandling.exceptionError(e, "error calling getSuccessor on predecessor");
			return false;
		}
	}

	protected void getNeighbours(int index) {

		assert neighbourhood_size <= nodes.size();

		int start = index - neighbourhood_size % 2;			//start point in array
		if( start < 0 ) {
			start = nodes.size() + start;	// fix start point up
		}
		int localindex = start;
		for( int i = 0; i < neighbourhood_size; i++ ) {
			if( localindex >= nodes.size() ) {
				localindex = 0;
			}
			if( localindex == index ) { // dont want to do the original node
				localindex++; 			// skip the original
				if (localindex >= nodes.size()) {
					localindex = 0;
				}
			}
			neighbours[i] = nodes.get(localindex++);
		}
	}

	/**
	 * Adds neighbour knowledge to each node in the simulation.
	 */
	public void associateNeighbours() {
		IChordNode[] neighbour_nodes = new IChordNode[neighbourhood_size];

		int count = 0;
		for (int i = 0; i < nodes.size(); i++)
			if (!nodes.get(i).isSimulatingFailure()) {

				assert neighbourhood_size <= nodes.size();

				int start = i - neighbourhood_size % 2;			//start point in array
				if( start < 0 ) {
					start = nodes.size() + start;	// fix start point up
				}
				int localindex = start;
				for( int j = 0; j < neighbourhood_size; j++ ) {
					if( localindex >= nodes.size() ) {
						localindex = 0;
					}
					if( localindex == i ) { // dont want to do the original node
						localindex++; 			// skip the original
						if (localindex >= nodes.size()) {
							localindex = 0;
						}
					}
					neighbour_nodes[j] = nodes.get(localindex++);
				}

				((INeighbourAwareChordNode) nodes.get(i)).addNeighbours( neighbour_nodes );

				showProgress(count++, progress_granularity, linebreak_granularity, DiagnosticLevel.RUN);
			}
	}

	public void populateFingerTables() {

		List<IP2PNode> node_list = new ArrayList<IP2PNode>(nodes_in_key_order); //ArrayList in key-order
		int count = 0;

		int startingIndex = node_list.size() - 1;

		SimProgressWindow p=null;
		if(showProgress) {
			p = new SimProgressWindow("Populating Finger Tables",1,nodes.size());
		}

		for (int i = startingIndex; i >= 0; i--) {

			IChordNode node = (IChordNode)node_list.get(i);

			if (!node.isSimulatingFailure()) {

				IFingerTable jcft = node.getFingerTable();
				jcft.fixAllFingers();

				showProgress(count++, progress_granularity, linebreak_granularity, DiagnosticLevel.RUN);

				if(showProgress) {
					p.incrementProgress();
				}
			}
		}

		if(showProgress) {
			p.dispose();
		}
	}

	@Override
	public void showNode(IChordNode node ) {
		System.out.println( "Showing node[" + node.getKey() +"]" );
		// if (node.isSimulatingFailure()) System.out.println("node simulating failure");
		try {
			System.out.println( "\tpredecessor = " + node.getPredecessor().getKey() );
		} catch( Exception e ) {
			System.out.println( "\tError looking up pred key" );
		}
		try {
			System.out.println( "\tsuccessor = " + node.getSuccessor().getKey() );
		} catch( Exception e ) {
			System.out.println( "\tError looking up succ key" );
		}
		try {
			System.out.println( "\tport = " + node.getAddress().getPort());
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		System.out.println( "\tfinger table:"+node.getFingerTable().toStringCompact());
	}


}
