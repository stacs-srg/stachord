/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of stachord, an independent implementation of         *
 * the Chord protocol (http://pdos.csail.mit.edu/chord/).                  *
 *                                                                         *
 * stachord is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * stachord is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with stachord.  If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                         *
 ***************************************************************************/

package uk.ac.standrews.cs.stachord.interfaces;

import java.net.InetSocketAddress;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.impl.NextHopResult;

/**
 * Defines remotely accessible Chord node functionality.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public interface IChordRemote {

    /**
     * Returns this node's key.
     *
     * @return this node's key
     * @throws RPCException if an error occurs during the remote call
     */
    IKey getKey() throws RPCException;

    /**
     * Returns this node's address.
     *
     * @return this node's address
     * @throws RPCException if an error occurs during the remote call
     */
    InetSocketAddress getAddress() throws RPCException;

    /**
     * Executes the routing protocol.
     *
     * @param key a key to be routed to
     * @return the node to which the key maps
     * @throws RPCException if an error occurs during the remote call
     */
    IChordRemoteReference lookup(IKey key) throws RPCException;

    /**
     * Returns this node's successor in the key space.
     *
     * @return this node's successor in the key space
     * @throws RPCException if an error occurs during the remote call
     */
    IChordRemoteReference getSuccessor() throws RPCException;

    /**
     * Returns this node's predecessor in the key space.
     *
     * @return this node's predecessor in the key space
     * @throws RPCException if an error occurs during the remote call
     */
    IChordRemoteReference getPredecessor() throws RPCException;

    /**
     * Notifies this node that a given node may be its predecessor.
     *
     * @param potential_predecessor a node that may be this node's most suitable predecessor
     * @throws RPCException if an error occurs during the remote call
     */
    void notify(IChordRemoteReference potential_predecessor) throws RPCException;

    /**
     * Joins this node to the ring of which the specified node is a member.
     *
     * @param node a node in a ring
     * @throws RPCException if an error occurs during the remote call
     */
    void join(IChordRemoteReference node) throws RPCException;

    /**
     * Returns this node's successor list.
     *
     * @return this node's successor list
     * @throws RPCException if an error occurs during the remote call
     */
    List<IChordRemoteReference> getSuccessorList() throws RPCException;

    /**
     * Returns this node's finger list.
     *
     * @return this node's finger list
     * @throws RPCException if an error occurs during the remote call
     */
    List<IChordRemoteReference> getFingerList() throws RPCException;

    /**
     * Returns the next hop towards the successor node of a given key.
     *
     * @param key a key
     * @return the next hop towards the successor of the specified key
     * @throws RPCException if an error occurs during the remote call
     */
    NextHopResult nextHop(IKey key) throws RPCException;

    /**
     * Controls whether predecessor maintenance should be performed.
     *
     * @param enabled true if predecessor maintenance should be performed
     * @throws RPCException if an error occurs during the remote call
     */
    void enablePredecessorMaintenance(boolean enabled) throws RPCException;

    /**
     * Controls whether ring stabilization should be performed.
     *
     * @param enabled true if ring stabilization should be performed
     * @throws RPCException if an error occurs during the remote call
     */
    void enableStabilization(boolean enabled) throws RPCException;

    /**
     * Controls whether peer-state maintenance should be performed.
     *
     * @param enabled true if peer-state maintenance should be performed
     * @throws RPCException if an error occurs during the remote call
     */
    void enablePeerStateMaintenance(boolean enabled) throws RPCException;

    /**
     * Notifies this node that a given node in its peer-state may have failed.
     *
     * @param node the node that is suspected to have failed
     * @throws RPCException if an error occurs during the remote call
     */
    void notifyFailure(IChordRemoteReference node) throws RPCException;

    /**
     * Returns a detailed description of this node's state.
     *
     * @return a detailed description of this node's state
     * @throws RPCException if an error occurs during the remote call
     */
    String toStringDetailed() throws RPCException;

    /**
     * Returns a brief description of this node's state.
     *
     * @return a brief description of this node's state
     * @throws RPCException if an error occurs during the remote call
     */
    String toStringTerse() throws RPCException;
}
