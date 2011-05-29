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

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * A specialised version of ChordRemoteReference, used in testing, which retains the local reference.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 */
public class ChordLocalReference implements IChordRemoteReference {

    private final IChordNode node;
    protected final IChordRemoteReference remote_reference;

    public ChordLocalReference(final IChordNode node, final IChordRemoteReference remote_reference) {

        this.node = node;
        this.remote_reference = remote_reference;
    }

    public IChordNode getNode() {

        return node;
    }

    @Override
    public void ping() throws RPCException {

        remote_reference.ping();
    }

    @Override
    public IKey getCachedKey() throws RPCException {

        return remote_reference.getCachedKey();
    }

    @Override
    public InetSocketAddress getCachedAddress() {

        return remote_reference.getCachedAddress();
    }

    @Override
    public IChordRemote getRemote() {

        return remote_reference.getRemote();
    }

    @Override
    public int hashCode() {

        return remote_reference.hashCode();
    }

    @Override
    public boolean equals(final Object o) {

        try {
            return o instanceof IChordRemoteReference && remote_reference.getCachedKey() != null && remote_reference.getCachedKey().equals(((IChordRemoteReference) o).getCachedKey());
        }
        catch (final RPCException e) {
            return false;
        }
    }

    @Override
    public String toString() {

        return remote_reference.toString();
    }
}
