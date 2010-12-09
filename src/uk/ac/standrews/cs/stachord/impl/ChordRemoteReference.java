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

import java.net.InetSocketAddress;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Holds a reference to a remote Chord node, with a locally cached copy of its key and IP address.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
class ChordRemoteReference implements IChordRemoteReference {

    private static final long serialVersionUID = -7911452718429786447L;

    private final IKey key;
    private final InetSocketAddress address;
    private final IChordRemote reference;

    public ChordRemoteReference(final IKey key, final IChordRemote reference) throws RemoteException {

        this.key = key;
        this.reference = reference;

        address = reference.getAddress();
    }

    @Override
    public IKey getKey() {

        return key;
    }

    @Override
    public InetSocketAddress getCachedAddress() {

        return address;
    }

    @Override
    public IChordRemote getRemote() {

        return reference;
    }

    @Override
    public int hashCode() {

        return key == null ? 0 : key.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        return o instanceof ChordRemoteReference && key.equals(((ChordRemoteReference) o).getKey());
    }

    @Override
    public String toString() {

        return getRemote().toString();
    }
}
