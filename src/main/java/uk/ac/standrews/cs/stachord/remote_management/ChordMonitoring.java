/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2011 Distributed Systems Architecture Research Group *
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
package uk.ac.standrews.cs.stachord.remote_management;

import java.util.HashSet;
import java.util.Set;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Utility class for Chord monitoring.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class ChordMonitoring {

    private ChordMonitoring() {

    }

    /**
     * Traverses the ring from the given node in the given direction, and returns the length of the cycle containing the given node, or zero if there is no such cycle.
     *
     * @param host_descriptor a ring node
     * @param forwards true if the ring should be traversed via successor pointers, false if it should be traversed via predecessor pointers
     * @return the length of the cycle containing the given node, or zero if the ring node is null or there is no such cycle.
     * @throws InterruptedException
     */
    public static int cycleLengthFrom(final IChordRemoteReference application_reference, final boolean forwards) throws InterruptedException {

        if (application_reference == null) { return 0; }

        // Record the nodes that have already been encountered.
        final Set<IChordRemoteReference> nodes_encountered = new HashSet<IChordRemoteReference>();

        int cycle_length = 0;
        IChordRemoteReference node = application_reference;

        while (!Thread.currentThread().isInterrupted()) {

            cycle_length++;

            try {
                node = forwards ? node.getRemote().getSuccessor() : node.getRemote().getPredecessor();
            }
            catch (final RPCException e) {

                // Error traversing the ring, so it is broken.
                return 0;
            }

            // If the node is null, then the cycle is broken.
            if (node == null) { return 0; }
            // If the node is the start node, then a cycle has been found.
            if (node.equals(application_reference)) { return cycle_length; }
            // If the node is not the start node and it has already been encountered, then there is a cycle but it doesn't contain the start node.
            if (nodes_encountered.contains(node)) { return 0; }
            nodes_encountered.add(node);
        }

        throw new InterruptedException();
    }
}
