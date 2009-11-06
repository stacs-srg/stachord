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
 * Created on Dec 9, 2004 at 11:53:22 AM.
 */
package uk.ac.standrews.cs.stachordRMI.interfaces;

import java.util.List;

import uk.ac.standrews.cs.nds.eventModel.IEventGenerator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.impl.RingSegment;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoPrecedingNodeException;

/**
 * Interface defining finger tables.
 */
public interface IFingerTable {

	/**
	 * Returns the node with the key most closely preceding the given key.
	 *
	 * @param k a key
	 * @return the node with the key most closely preceding k
	 * @throws NoPrecedingNodeException if no such node is found
	 */
	IChordRemote closestPrecedingNode(IKey k) throws NoPrecedingNodeException;

	/**
	 * @return a list of references to finger nodes
	 */
	List<IChordRemote> getFingers();

	/**
	 * Fixes all the entries in the finger table.
	 */
	void fixAllFingers();

	/**
	 * Fixes the next entry in the finger table.
	 */
	void fixNextFinger();

	/**
	 * Sets the eventGenerator
	 * @param eventGenerator the GAM eventGenerator
	 */
	void setEventGenerator(IEventGenerator eventGenerator);

	/**
	 * Notifies this finger table of a node that could be added.
	 * This is soft state primarily used to accelerate the initiation of systems.
	 * No position is taken on what the finger table implementations do with this information.
	 * This may also be used to incorporate nodes that are encountered opportunistically during run.
	 * 
	 * @param extantNode a node that could be added to the finger table
	 */
	void notifyExistence(IChordRemote extantNode);

	/**
	 * Notifies this finger table of a node that could be removed.
	 * This may also be used to improve performance by suggesting that nodes (perhaps known to be dead) be removed.
	 * 
	 * @param deadNode the node that could be removed from the finger table
	 */
	void notifySuspectedFailure(IChordRemote deadNode);

	/**
	 * @return a string representing the contents of the finger table omitting duplicate keys
	 */
	String toStringCompact();

	/**
	 * @return the number of entries in the finger table
	 */
	int size();
}