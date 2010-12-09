/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
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

import java.util.Observer;

import uk.ac.standrews.cs.nds.events.Event;
import uk.ac.standrews.cs.nds.events.IEvent;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

/**
 * Defines locally accessible Chord node functionality.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public interface IChordNode extends Observer {

    /**
     * The name of the remotely accessible Chord service.
     */
    String CHORD_REMOTE_SERVICE_NAME = IChordRemote.class.getSimpleName();

    /**
     * Predecessor change event.
     */
    IEvent PREDECESSOR_CHANGE_EVENT = new Event("PREDECESSOR_CHANGE_EVENT");

    /**
     * Successor change event.
     */
    IEvent SUCCESSOR_CHANGE_EVENT = new Event("SUCCESSOR_CHANGE_EVENT");

    /**
     * Successor list change event.
     */
    IEvent SUCCESSOR_LIST_CHANGE_EVENT = new Event("SUCCESSOR_LIST_CHANGE_EVENT");

    /**
     * Finger table change event.
     */
    IEvent FINGER_TABLE_CHANGE_EVENT = new Event("FINGER_TABLE_CHANGE_EVENT");

    /**
     * Event for this chord nodes own address being changed.
     */
    IEvent OWN_ADDRESS_CHANGE_EVENT = new Event("OWN_ADDRESS_CHANGE_EVENT");

    /**
     * The maximum length of a node's successor list.
     */
    int MAX_SUCCESSOR_LIST_SIZE = 5;

    /**
     * The default RMI registry port.
     */
    int DEFAULT_RMI_REGISTRY_PORT = 1099;

    /**
     * The ratio between successive finger target keys in the finger table.
     */
    int INTER_FINGER_RATIO = 2;

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Shared functionality with IChordRemote.

    /**
     * Returns this node's key.
     * @return this node's key
     */
    IKey getKey();

    /**
     * Executes the routing protocol.
     *
     * @param key a key to be routed to
     * @return the node to which the key maps
     * @throws RemoteException if an error occurs during the routing protocol
     */
    IChordRemoteReference lookup(IKey key) throws RemoteException;

    /**
     * Returns this node's successor in the key space.
     * @return this node's successor in the key space
     */
    IChordRemoteReference getSuccessor();

    /**
     * Returns this node's predecessor in the key space.
     * @return this node's predecessor in the key space
     */
    IChordRemoteReference getPredecessor();

    /**
     * Joins this node to the ring of which the specified node is a member.
     *
     * @param node a node in a ring
     * @throws RemoteException if an error occurs during the remote call
     */
    void join(IChordRemoteReference node) throws RemoteException;

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Local-only functionality.

    /**
     * Returns a reference to this node typed as a remote reference.
     * @return a reference to this node typed as a remote reference
     */
    IChordRemoteReference getSelfReference();

    /**
     * Stops maintenance operations and removes the node from remote access.
     */
    void shutDown();

    /**
     * Adds an observer.
     *
     * @param observer a new observer
     */
    void addObserver(Observer observer);

    /**
     * Checks whether the given key lies in this node's key range.
     *
     * @param k a key
     * @return true if the key lies in this node's key range
     */
    boolean inLocalKeyRange(final IKey k);
}
