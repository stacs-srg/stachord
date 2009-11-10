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

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PNode;

/**
 * Defines locally accessible Chord node functionality.
 *
 * @author graham
 */
public interface IChordNode { // extends IChordRemote {
	/**
	 * Creates a new ring containing only this node.
	 */
	void createRing();

	/**
	 * Joins an existing ring containing a specified known node.
	 *
	 * @param known_node a node on an existing ring
	 * @throws RemoteException 
	 * @throws P2PNodeException if a failure occurs during the join protocol
	 */
	boolean join(IChordRemoteReference known_node) throws RemoteException;

	/**
	 * Executes the stabilization protocol.
	 * If the predecessor of this node's current successor is not this node, a new node has joined between this node and
	 * this node's successor. If the new node in between has a key that is
	 * between this node's key, and its successor's key, this node will set its
	 * successor to be the new node and will call the new node's notify method
	 * to tell the new node that it is its predecessor. If the new node is not
	 * in between, this node will call notify on the existing successor telling
	 * it to set its predecessor back to this node.
	 */
	void stabilize();

	/**
	 * Check whether the node's predecessor has failed, and drops reference if so.
	 */
	void checkPredecessor();

	/**
	 * Updates an entry in this node's finger table.
	 */
	void fixNextFinger();

	/**
	 * Updates an entry in this node's finger table.
	 */
	void fixAllFingers();
	
	/**
	 * Sets the predecessor node in key space.
	 *
	 * @param p the new predecessor node
	 */
	void setPredecessor(IChordRemoteReference p);

	/**
	 * Allows node failure to be simulated.
	 *
	 * @param failed
	 */
	void setSimulatingFailure(boolean failed);

	/**
	 * @return this node's finger table
	 */
	IFingerTable getFingerTable();

	/**
	 * Determines whether the specified key lies in this node's key range.
	 * 
	 * @param k a key
	 * @return true if k lies in this node's key range
	 * @throws  
	 * @throws P2PNodeException if this node's key range is unknown due to incomplete peer state.
	 */
	boolean inLocalKeyRange(IKey k) throws P2PNodeException ;
	
	IChordRemoteReference lookup( IKey key ) throws RemoteException;
	
	InetSocketAddress getAddress();

	IKey getKey();

	IChordRemoteReference getSuccessor();
	
	IChordRemoteReference getProxy();
	
	void showState();
}
