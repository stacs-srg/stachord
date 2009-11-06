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
package uk.ac.standrews.cs.stachordRMI.interfaces;

import java.util.List;

import uk.ac.standrews.cs.nds.p2p.interfaces.IApplicationComponentLocator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PNode;
import uk.ac.standrews.cs.nds.util.Pair;
import uk.ac.standrews.cs.stachordRMI.impl.NextHopResultStatus;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines remotely accessible Chord node functionality.
 *
 * @author graham
 */
public interface IChordRemote extends IP2PNode, IApplicationComponentLocator, Remote {

	/**
	 * Notifies this node that a given node may be its predecessor.
	 *
	 * @param potential_predecessor a node that may be this node's most suitable predecessor
	 */
	void notify(IChordRemote potential_predecessor) throws RemoteException;

	/**
	 * Returns this node's successor list.
	 *
	 * @return this node's successor list
	 */
	List<IChordRemote> getSuccessorList() throws RemoteException;

	/**
	 * Returns this node's finger list.
	 *
	 * @return this node's finger list
	 */
	List<IChordRemote> getFingerList() throws RemoteException;

	/**
	 * Returns this node's predecessor in the key space.
	 * 
	 * @return this node's predecessor node
	 */
	IChordRemote getPredecessor() throws RemoteException;

	/**
	 * Returns this node's successor in the key space.
	 * 
	 * @return this node's successor node
	 */
	IChordRemote getSuccessor() throws RemoteException;

	/**
	 * Used to check the availability of this node.
	 */
	void isAlive() throws RemoteException;

	/**
	 * Returns the next hop towards the successor node of a given key.
	 * This corresponds to the finger that extends the furthest across the key space
	 * without overshooting the target key.
	 * 
	 * @param k a key
	 * @return the next hop towards the successor of the specified key
	 */
	Pair<NextHopResultStatus, IChordRemote> nextHop(IKey k);
}