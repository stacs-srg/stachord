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
package uk.ac.standrews.cs.stachordRMI.simulation;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTableFactory;

/**
 * Implementation of simulated Chord node.
 */
public class ChordSimulationNode extends ChordNodeImpl implements INeighbourAwareChordNode {

	public ChordSimulationNode(InetSocketAddress hostAddress, IKey key, IFingerTableFactory finger_table_factory) {

		super(hostAddress, key, finger_table_factory);
	}

	public void addNeighbours(IChordNode[] neighbours) {

		for (IChordNode suggestion : neighbours)
			if (suggestion != this && suggestion != getSuccessor()) {
				getFingerTable().notifyExistence(suggestion);
			}
	}
}
