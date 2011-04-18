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

package uk.ac.standrews.cs.stachord.remote_management;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.p2p.network.P2PNodeFactory;
import uk.ac.standrews.cs.nds.p2p.network.P2PNodeManager;
import uk.ac.standrews.cs.nds.registry.IRegistry;
import uk.ac.standrews.cs.nds.registry.LocateRegistry;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.impl.ChordRemoteServer;
import uk.ac.standrews.cs.stachord.servers.NodeServer;

/**
 * Provides remote management hooks for Chord.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ChordManager extends P2PNodeManager {

    private final ChordNodeFactory factory;

    /**
     * Name of 'ring size' attribute.
     */
    public static final String RING_SIZE_NAME = "Ring Size";

    private static final String CHORD_APPLICATION_NAME = "Chord";

    /**
     * Initializes a Chord manager for remote deployment.
     */
    public ChordManager() {

        this(false);
    }

    /**
     * Initializes a Chord manager.
     * 
     * @param local_deployment_only true if nodes are only to be deployed to the local node
     */
    public ChordManager(final boolean local_deployment_only) {

        super(local_deployment_only);

        factory = new ChordNodeFactory();

        getSingleScanners().add(new ChordCycleLengthScanner());
        getGlobalScanners().add(new ChordPartitionScanner());
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String getApplicationName() {

        return CHORD_APPLICATION_NAME;
    }

    @Override
    public void establishApplicationReference(final HostDescriptor host_descriptor) throws Exception {

        final InetSocketAddress inet_socket_address = host_descriptor.getInetSocketAddress();

        try {
            host_descriptor.applicationReference(factory.bindToNode(inet_socket_address));
        }
        catch (final Exception e) {

            // Try accessing Chord via the registry.
            final InetAddress address = inet_socket_address.getAddress();
            final IRegistry registry = LocateRegistry.getRegistry(address);
            final int chord_port = registry.lookup(ChordRemoteServer.DEFAULT_REGISTRY_KEY);

            host_descriptor.applicationReference(factory.bindToNode(new InetSocketAddress(address, chord_port)));
            host_descriptor.port(chord_port);
        }
    }

    @Override
    public void killApplication(final HostDescriptor host_descriptor) throws Exception {

        // Check whether a process handle exists, implying that the node was initiated by the current Java process.
        if (host_descriptor.getNumberOfProcesses() > 0) {
            super.killApplication(host_descriptor);
        }
        else {
            // If not, try to kill the process by guessing the format of the process name.
            final String match_fragment = NodeServer.class.getName() + " -s" + host_descriptor.getInetAddress().getCanonicalHostName() + ":" + host_descriptor.getPort();
            host_descriptor.getProcessManager().killMatchingProcesses(match_fragment);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    protected P2PNodeFactory getP2PNodeFactory() {

        return factory;
    }
}
