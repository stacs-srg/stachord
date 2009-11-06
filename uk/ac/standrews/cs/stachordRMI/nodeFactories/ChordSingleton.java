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
 * Created on 02-Nov-2004
 */
package uk.ac.standrews.cs.stachordRMI.nodeFactories;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.eventModel.IEventGenerator;
import uk.ac.standrews.cs.nds.eventModel.eventBus.busInterfaces.IEventBus;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.P2PStatus;
import uk.ac.standrews.cs.nds.p2p.interfaces.IApplicationRegistry;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;

/**
 * @author stuart, graham
 */
public class ChordSingleton {

	private static IChordNode instance = null;

	public static IChordNode getInstance() {

		if (ChordSingleton.instance == null) {
			ErrorHandling.error("The ChordNode has not been initialised");
		}

		return instance;
	}

	public static IChordNode initialise(InetSocketAddress node_rep, IKey key, IChordRemote known_node, IEventBus bus, IApplicationRegistry registry  ) throws P2PNodeException {

		if (ChordSingleton.instance != null) {
			throw new P2PNodeException(P2PStatus.INSTANTIATION_FAILURE,"The ChordNode has already been initialised");
		} else {

			instance = new ChordNodeImpl( node_rep, key, bus, registry );

			if (known_node == null) {
				instance.createRing();
			} else {
				instance.join(known_node);
			}
		}
		return instance;
	}
}
