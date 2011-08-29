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

import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Binds together a reference to a remote node and a flag indicating whether that node is the last hop in an invocation of the routing protocol.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class NextHopResult {

    private final boolean is_final_hop;
    private final IChordRemoteReference node;

    /**
     * Constructs a new record.
     * @param node the node
     * @param is_final_hop the flag
     */
    public NextHopResult(final IChordRemoteReference node, final boolean is_final_hop) {

        this.is_final_hop = is_final_hop;
        this.node = node;
    }

    /**
     * Returns true if the node is the final hop.
     * @return true if the node is the final hop
     */
    public boolean isFinalHop() {

        return is_final_hop;
    }

    /**
     * Returns the node.
     * @return the node
     */
    public IChordRemoteReference getNode() {

        return node;
    }
}
