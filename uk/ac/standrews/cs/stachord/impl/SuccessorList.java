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

package uk.ac.standrews.cs.stachord.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Successor list implementation.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
class SuccessorList {

    private final ChordNodeImpl node;
    private final List<IChordRemoteReference> successor_list;

    // -------------------------------------------------------------------------------------------------------

    public SuccessorList(final ChordNodeImpl local_node) {

        node = local_node;
        successor_list = new ArrayList<IChordRemoteReference>();
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Searches the successor list for a working node.
     * 
     * @return the first working node in the successor list
     * @throws NoReachableNodeException if no working node is found
     */
    protected IChordRemoteReference findFirstWorkingNode() throws NoReachableNodeException {

        for (final IChordRemoteReference next : successor_list) {
            try {
                next.getRemote().isAlive();
                return next;
            }
            catch (final RemoteException e) {
            }
        }
        throw new NoReachableNodeException();
    }

    /**
     * Returns the successor list.
     * @return the successor list
     */
    protected List<IChordRemoteReference> getList() {

        return successor_list;
    }

    /**
     * Clears the successor list.
     */
    protected void clear() {

        successor_list.clear();
    }

    /**
     * Constructs a new successor list which consists of this node's successor
     * followed by the first (MAX_SIZE-1) elements of the successor's successor
     * list.
     */
    protected boolean refreshList(final List<IChordRemoteReference> successor_list_of_successor) {

        final IChordRemoteReference successor = node.getSuccessor();

        final List<IChordRemoteReference> new_list = new ArrayList<IChordRemoteReference>();

        final int number_to_be_taken_from_successors_successor_list = Math.min(Constants.MAX_SUCCESSOR_LIST_SIZE - 1, successor_list_of_successor.size());

        // Check for the element of the successor list being this node, as will
        // happen with a small number of nodes in the ring. If this node is
        // found in the received successor list then that element and all
        // elements following it are discarded.

        for (int i = 0; i < number_to_be_taken_from_successors_successor_list; i++) {

            final IChordRemoteReference successor_list_node = successor_list_of_successor.get(i);

            // Check for wrap-around in small ring.
            if (successor_list_node.getKey().equals(node.getKey())) {
                break;
            }

            new_list.add(successor_list_node);
        }

        // Add the successor at the front of the new list.
        new_list.add(0, successor);

        if (!new_list.equals(successor_list)) {

            successor_list.clear();
            successor_list.addAll(new_list);
            return true;
        }

        return false;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {

        final StringBuilder buffer = new StringBuilder();
        buffer.append("\n");

        if (successor_list.isEmpty()) {
            buffer.append("empty");
        }
        else {
            for (final IChordRemoteReference successor : successor_list) {

                buffer.append("successor: ");
                buffer.append(successor != null ? successor.getKey() : "null");
                buffer.append(" address: ");
                buffer.append(successor != null ? successor.getAddress() : "null");
                buffer.append("\n");
            }
        }

        return buffer.toString();
    }
}
