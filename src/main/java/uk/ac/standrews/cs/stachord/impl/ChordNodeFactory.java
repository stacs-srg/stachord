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

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.utilities.archive.Duration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides methods for creating new Chord nodes and binding to existing remote Chord nodes.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class ChordNodeFactory {

    protected static final Duration INDIVIDUAL_TIMEOUT_INTERVAL = new Duration(50, TimeUnit.SECONDS); // Timeout for individual connection attempt.
    protected static final Duration RETRY_INTERVAL = new Duration(1, TimeUnit.SECONDS); // Interval between retry of connecting to remote nodes.

    public ChordNodeFactory() {

        super();
    }

    /**
     * Creates a new Chord node running in the current JVM at a given local network address on a given port, establishing a new one-node ring.
     *
     * @param local_address the local address of the node
     * @return the new node
     *
     * @throws IOException if the node cannot bind to the specified local address
     * @throws RPCException if an error occurs binding the node to the registry
     * @throws AlreadyBoundException if another node is already bound in the registry
     * @throws RegistryUnavailableException if the registry is unavailable
     * @throws TimeoutException
     * @throws InterruptedException
     */
    public IChordNode createNode(final InetSocketAddress local_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        return new ChordNodeImpl(local_address);
    }

    /**
     * Creates a new Chord node running in the current JVM at a given local network address on a given port, with a given key, establishing a new one-node ring.
     *
     * @param local_address the local address of the node
     * @param key the key of the new node
     * @return the new node
     *
     * @throws IOException if the node cannot bind to the specified local address
     * @throws RPCException if an error occurs binding the node to the registry
     * @throws AlreadyBoundException if another node is already bound in the registry
     * @throws RegistryUnavailableException if the registry is unavailable
     * @throws TimeoutException
     * @throws InterruptedException
     */
    public IChordNode createNode(final InetSocketAddress local_address, final IKey key) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        return new ChordNodeImpl(local_address, key);
    }

    /**
     * Binds to an existing remote Chord node running at a given network address, checking for liveness.
     *
     * @param node_address the address of the existing node
     * @return a remote reference to the node
     *
     * @throws RPCException if an error occurs communicating with the remote machine
     */
    public IChordRemoteReference bindToNode(final InetSocketAddress node_address) throws RPCException {

        final ChordRemoteReference remote_reference = new ChordRemoteReference(node_address);

        // Check that the remote application can be contacted.
        remote_reference.ping();

        return remote_reference;
    }
}
