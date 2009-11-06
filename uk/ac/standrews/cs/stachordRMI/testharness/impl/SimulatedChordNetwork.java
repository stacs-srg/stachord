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
package uk.ac.standrews.cs.stachordRMI.testharness.impl;

import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.interfaces.INodeFactory;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.fingerTableFactories.GeometricFingerTableFactory;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.simulation.ChordSimulation;
import uk.ac.standrews.cs.stachordRMI.simulation.ChordSimulationNodeFactory;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetwork;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetworkNodeHandle;

public class SimulatedChordNetwork implements IChordNetwork<IChordNode>{

	private ChordSimulation chord_simulation;
	private final INodeFactory<IChordNode> node_factory;

	private SimulatedChordNetwork() {

		node_factory = new ChordSimulationNodeFactory(new GeometricFingerTableFactory());
	}

	public SimulatedChordNetwork(int n) {

		this();
		chord_simulation = new ChordSimulation(n,0, node_factory);
		chord_simulation.initialiseP2PLinks();
	}

	public SimulatedChordNetwork(String[] keyStrings) {

		this();
		chord_simulation = new ChordSimulation(keyStrings, 0,node_factory);
		chord_simulation.initialiseP2PLinks();
	}

	private List<IChordNetworkNodeHandle<IChordNode>> makeNodeList(){
		List<IChordNetworkNodeHandle<IChordNode>> nodeHandles=new ArrayList<IChordNetworkNodeHandle<IChordNode>>();

		for(IChordNode chordnode:chord_simulation.getNodes()) {
			nodeHandles.add(new ChordSimulationNodeHandle(chordnode,this));
		}

		return nodeHandles;
	}

	public void addNode(String k) throws P2PNodeException {
		chord_simulation.addNodeToRing(k);
	}

	public void addNode() throws P2PNodeException {
		chord_simulation.addNodeToRing();
	}

	public List<IChordNetworkNodeHandle<IChordNode>> getNodes() {
		return makeNodeList();
	}

	public void killNetwork() {
		for(IChordNetworkNodeHandle<IChordNode> n:makeNodeList()) {
			n.killNode();
		}
	}

	public void killNode(IChordNetworkNodeHandle<IChordNode> node) {
		chord_simulation.removeNodeFromRing(node.getNode());
	}

	public void waitForStableNetwork() {

		List<IChordNode> nodes = chord_simulation.getNodes();
		while (!isNetworkStable()) {
			for (IChordNode n:nodes)
				if(n.isSimulatingFailure()) {
					ErrorHandling.hardError("A failed node was found in the network node set.");
				} else {
					n.stabilize();
				}
		}
	}

	public static void main(String[] args) {
		System.out.println();
		@SuppressWarnings("unused")
		SimulatedChordNetwork simNet = new SimulatedChordNetwork(3);
	}

	public boolean isNetworkStable() {
		return chord_simulation.isRingStable();
	}
}
