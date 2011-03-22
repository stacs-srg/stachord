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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.network.P2PNodeFactory;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.servers.NodeServer;

/**
 * Provides methods for creating new Chord nodes and binding to existing remote Chord nodes.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class ChordNodeFactory extends P2PNodeFactory {

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
     */
    public IChordNode createNode(final InetSocketAddress local_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException {

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
     */
    public IChordNode createNode(final InetSocketAddress local_address, final IKey key) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException {

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

    /**
     * Binds to an existing remote Chord node running at a given network address, checking for liveness, retrying on any error until the timeout interval has elapsed.
     *
     * @param node_address the address of the existing node
     * @return a remote reference to the node
     *
     * @throws TimeoutException if the node cannot be bound to within the timeout interval
     */
    public IChordRemoteReference bindToNode(final InetSocketAddress node_address, final int retry_interval, final int timeout_interval) throws TimeoutException {

        return (IChordRemoteReference) bindToNode(retry_interval, timeout_interval, node_address);
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    protected IChordRemoteReference createLocalReference(final Object node, final Object remote_reference) {

        return new ChordLocalReference((IChordNode) node, (IChordRemoteReference) remote_reference);
    }

    @Override
    public Object bindToNode(final Object... args) throws RPCException {

        final InetSocketAddress node_address = (InetSocketAddress) args[0];

        return bindToNode(node_address);
    }

    @Override
    protected Object bindToNode(final HostDescriptor host_descriptor) throws UnknownHostException, TimeoutException {

        return bindToNode(host_descriptor.getInetSocketAddress(), RETRY_INTERVAL, TIMEOUT_INTERVAL);
    }

    @Override
    protected Class<?> getNodeServerClass() {

        return NodeServer.class;
    }
}
