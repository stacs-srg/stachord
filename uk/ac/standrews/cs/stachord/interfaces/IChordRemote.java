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
package uk.ac.standrews.cs.stachord.interfaces;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachord.impl.NextHopResult;

/**
 * Defines remotely accessible Chord node functionality.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public interface IChordRemote extends Remote {

	/**
	 * Returns this node's key.
	 * 
	 * @return this node's key
	 * @throws RemoteException if an error occurs during the remote call
	 */
	IKey getKey() throws RemoteException;
	
	/**
	 * Returns this node's address.
	 * 
	 * @return this node's address
	 * @throws RemoteException if an error occurs during the remote call
	 */
	InetSocketAddress getAddress() throws RemoteException;
	
	/**
	 * Executes the routing protocol.
	 * 
	 * @param key a key to be routed to
	 * @return the node to which the key maps
	 * @throws RemoteException if an error occurs during the remote call
	 */
	IChordRemoteReference lookup(IKey key) throws RemoteException;

	/**
	 * Returns this node's successor in the key space.
	 * 
	 * @return this node's successor in the key space
	 * @throws RemoteException if an error occurs during the remote call
	 */
	IChordRemoteReference getSuccessor() throws RemoteException;

	/**
	 * Returns this node's predecessor in the key space.
	 * 
	 * @return this node's predecessor in the key space
	 * @throws RemoteException if an error occurs during the remote call
	 */
	IChordRemoteReference getPredecessor() throws RemoteException;

	/**
	 * Notifies this node that a given node may be its predecessor.
	 *
	 * @param potential_predecessor a node that may be this node's most suitable predecessor
	 * @throws RemoteException if an error occurs during the remote call
	 */
	void notify(IChordRemoteReference potential_predecessor) throws RemoteException;
	
	/**
	 * Joins this node to the ring of which the specified node is a member.
	 * 
	 * @param node a node in a ring
	 * @throws RemoteException if an error occurs during the remote call
	 */
	void join(IChordRemoteReference node) throws RemoteException;

	/**
	 * Returns this node's successor list.
	 * 
	 * @return this node's successor list
	 * @throws RemoteException if an error occurs during the remote call
	 */
	ArrayList<IChordRemoteReference> getSuccessorList() throws RemoteException;

	/**
	 * Returns this node's finger list.
	 * 
	 * @return this node's finger list
	 * @throws RemoteException if an error occurs during the remote call
	 */
	ArrayList<IChordRemoteReference> getFingerList() throws RemoteException;

	/**
	 * Used to check liveness of this node.
	 * 
	 * @throws RemoteException if an error occurs during the remote call
	 */
	void isAlive() throws RemoteException;

	/**
	 * Returns the next hop towards the successor node of a given key.
	 * Of the nodes known by this node, the result is the node whose key is the furthest round the key space
	 * without overshooting the target key.
	 * 
	 * @param key a key
	 * @return the next hop towards the successor of the specified key
	 * @throws RemoteException if an error occurs during the remote call
	 */
	NextHopResult nextHop(IKey key) throws RemoteException;
	
	/**
	 * Controls whether predecessor maintenance should be performed.
	 * 
	 * @param enabled true if predecessor maintenance should be performed
	 * @throws RemoteException if an error occurs during the remote call
	 */
	void enablePredecessorMaintenance(boolean enabled) throws RemoteException;

	/**
	 * Controls whether ring stabilization should be performed.
	 * 
	 * @param enabled true if ring stabilization should be performed
	 * @throws RemoteException if an error occurs during the remote call
	 */
	void enableStabilization(boolean enabled) throws RemoteException;

	/**
	 * Controls whether peer-state maintenance should be performed.
	 * 
	 * @param enabled true if peer-state maintenance should be performed
	 * @throws RemoteException if an error occurs during the remote call
	 */
	void enablePeerStateMaintenance(boolean enabled) throws RemoteException;

	/**
	 * Notifies this node that a given node in its peer-state may have failed.
	 * 
	 * @param node the node that is suspected to have failed
	 * @throws RemoteException if an error occurs during the remote call
	 */
	void notifyFailure(IChordRemoteReference node) throws RemoteException;

	/**
	 * Returns a detailed description of this node's state.
	 * 
	 * @return a detailed description of this node's state
	 * @throws RemoteException if an error occurs during the remote call
	 */
	String toStringDetailed() throws RemoteException;
	
	/**
	 * Returns a brief description of this node's state.
	 * 
	 * @return a brief description of this node's state
	 * @throws RemoteException if an error occurs during the remote call
	 */
	String toStringTerse() throws RemoteException;
}
