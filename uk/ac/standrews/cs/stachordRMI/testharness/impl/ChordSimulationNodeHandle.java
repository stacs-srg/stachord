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

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetworkNodeHandle;

/**
 * Defines functionality of a Chord node manipulated by test harness, running IChordNode application.
 */
public class ChordSimulationNodeHandle implements IChordNetworkNodeHandle<IChordNode> {

	private final IChordNode chord_node;
	private final SimulatedChordNetwork simulated_network;

	/**
	 * @param chord_node a Chord node
	 * @param simulated_network a network
	 */
	public ChordSimulationNodeHandle(IChordNode chord_node, SimulatedChordNetwork simulated_network) {

		this.chord_node = chord_node;
		this.simulated_network = simulated_network;
	}

	public IChordRemote getChordNode() {
		return chord_node;
	}

	public IChordNode getNode() {
		return chord_node;
	}

	public void killNode() {
		simulated_network.killNode(this);
	}
}
