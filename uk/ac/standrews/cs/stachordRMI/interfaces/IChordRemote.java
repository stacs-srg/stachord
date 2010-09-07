/*******************************************************************************
 * StAChord Library
 * Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 * http://asa.cs.st-andrews.ac.uk/
 * 
 * This file is part of stachordRMI.
 * 
 * stachordRMI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * stachordRMI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with stachordRMI.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.standrews.cs.stachordRMI.interfaces;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.impl.NextHopResult;

/**
 * Defines remotely accessible Chord node functionality.
 *
 * @author graham
 */
public interface IChordRemote extends Remote {

	/**
	 * Get the key of the node
	 */
	public IKey getKey() throws RemoteException;
	
	/**
	 * Get the address of a node
	 */
	public InetSocketAddress getAddress() throws RemoteException;
	
	/**
	 * Notifies this node that a given node may be its predecessor.
	 *
	 * @param potential_predecessor a node that may be this node's most suitable predecessor
	 */
	void notify(IChordRemoteReference potential_predecessor) throws RemoteException;

	/**
	 * Returns this node's successor list.
	 *
	 * @return this node's successor list
	 */
	ArrayList<IChordRemoteReference> getSuccessorList() throws RemoteException;

	/**
	 * Returns this node's finger list.
	 *
	 * @return this node's finger list
	 */
	ArrayList<IChordRemoteReference> getFingerList() throws RemoteException;

	/**
	 * Returns this node's predecessor in the key space.
	 * 
	 * @return this node's predecessor node
	 */
	IChordRemoteReference getPredecessor() throws RemoteException;

	/**
	 * Returns this node's successor in the key space.
	 * 
	 * @return this node's successor node
	 */
	IChordRemoteReference getSuccessor() throws RemoteException;

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
	NextHopResult nextHop(IKey k) throws RemoteException;
	
	IChordRemoteReference lookup(IKey key) throws RemoteException;

	public void enableFingerTableMaintenance(boolean enabled) throws RemoteException;

	public void fingerFailure(IChordRemoteReference broken_finger) throws RemoteException;

	String toStringDetailed() throws RemoteException;
	
	String toStringTerse() throws RemoteException;
}
