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

import java.io.Serializable;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

/**
 * Holds a reference to a remote Chord node, with a cached key and address.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 */
public interface IChordRemoteReference extends Serializable {

    /**
     * Returns the key associated with this reference.
     *
     * @return the key associated with this reference
     */
    IKey getKey();

    /**
     * Returns the address associated with this reference.
     *
     * @return the address associated with this reference
     */
    InetSocketAddress getCachedAddress();

    /**
     * Returns the remote reference.
     *
     * @return the remote reference
     */
    IChordRemote getRemote();
}
