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
 * Created on Jan 18, 2005 at 5:07:00 PM.
 */
package uk.ac.standrews.cs.stachordRMI.simulation;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.INodeFactory;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTableFactory;

public class ChordSimulationNodeFactory implements INodeFactory<IChordNode> {

	private IFingerTableFactory finger_table_factory = null;

	public ChordSimulationNodeFactory(IFingerTableFactory finger_table_factory) {
		this.finger_table_factory = finger_table_factory;
	}

	public IChordNode makeNode(InetSocketAddress hostAddress, IKey key) {

		return new ChordSimulationNode(hostAddress, key, finger_table_factory);
	}
}