/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland
 * http://beast.cs.st-andrews.ac.uk/                                 *
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

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Holds a reference to a remote Chord node, with a locally cached copy of its key and IP address.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 */
class ChordRemoteReference implements IChordRemoteReference {

    private IKey key = null;
    private final InetSocketAddress address;
    private final ChordRemoteProxy reference;

    public ChordRemoteReference(final InetSocketAddress address) {

        this.address = address;
        reference = ChordRemoteProxy.getProxy(address);
    }

    public ChordRemoteReference(final IKey key, final InetSocketAddress address) {

        this(address);
        this.key = key;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public IKey getCachedKey() throws RPCException {

        if (key == null) {
            key = reference.getKey();
        }
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
    public void ping() throws RPCException {

        reference.ping();
    }

    @Override
    public int hashCode() {

        return key == null ? 0 : key.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        try {
            return o instanceof IChordRemoteReference && key != null && key.equals(((IChordRemoteReference) o).getCachedKey());
        }
        catch (final RPCException e) {
            return false;
        }
    }

    @Override
    public String toString() {

        return getRemote().toString();
    }
}
