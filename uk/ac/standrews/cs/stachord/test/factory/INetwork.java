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

package uk.ac.standrews.cs.stachord.test.factory;

import java.util.List;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;

/**
 * Interface representing a set of Chord nodes.
 *
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public interface INetwork {

    /**
     * Returns a new list containing the nodes.
     * @return the nodes in the network, sorted in ascending key order.
     */
    List<HostDescriptor> getNodes();

    /**
     * Kills a given node and removes it from the network.
     * @param node the node to be killed
     */
    void killNode(HostDescriptor node);

    /**
     * Kills all nodes and removes them from the network.
     */
    void killAllNodes();
}
