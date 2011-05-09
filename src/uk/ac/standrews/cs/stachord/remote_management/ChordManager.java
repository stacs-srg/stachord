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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.p2p.network.P2PNodeFactory;
import uk.ac.standrews.cs.nds.p2p.network.P2PNodeManager;
import uk.ac.standrews.cs.nds.registry.IRegistry;
import uk.ac.standrews.cs.nds.registry.LocateRegistry;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.impl.ChordRemoteServer;
import uk.ac.standrews.cs.stachord.servers.NodeServer;

/**
 * Provides remote management hooks for Chord.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ChordManager extends P2PNodeManager {

    private static final String LOCAL_HOSTNAME_SUFFIX = ".local";

    private final ChordNodeFactory factory;

    private final boolean try_registry_on_connection_error;

    /**
     * Name of 'ring size' attribute.
     */
    public static final String RING_SIZE_NAME = "Ring Size";

    private static final String CHORD_APPLICATION_NAME = "Chord";

    private static final Duration CHORD_CONNECTION_RETRY = new Duration(5, TimeUnit.SECONDS);
    private static final Duration CHORD_CONNECTION_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    /**
     * Initializes a Chord manager for remote deployment.
     */
    public ChordManager() {

        this(false, true, true);
    }

    /**
     * Initializes a Chord manager.
     * 
     * @param local_deployment_only true if nodes are only to be deployed to the local node
     */
    public ChordManager(final boolean local_deployment_only, final boolean run_chord_scanners, final boolean try_registry_on_connection_error) {

        super(local_deployment_only);

        this.try_registry_on_connection_error = try_registry_on_connection_error;

        factory = new ChordNodeFactory();

        if (run_chord_scanners) {
            getSingleScanners().add(new ChordCycleLengthScanner());
            getGlobalScanners().add(new ChordPartitionScanner());
        }
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String getApplicationName() {

        return CHORD_APPLICATION_NAME;
    }

    AtomicInteger conc = new AtomicInteger(0);

    @Override
    public void establishApplicationReference(final HostDescriptor host_descriptor) throws Exception {

        //        System.out.println("concurrency in ear now: " + conc.incrementAndGet());

        try {
            final InetSocketAddress inet_socket_address = host_descriptor.getInetSocketAddress();

            if (inet_socket_address.getPort() == 0) {

                if (try_registry_on_connection_error) {
                    establishApplicationReferenceViaRegistry(host_descriptor, inet_socket_address);
                }
                else {
                    throw new Exception("trying to establish connection with port 0 and registry retry disabled");
                }
            }

            try {
                host_descriptor.applicationReference(factory.bindToNode(inet_socket_address, CHORD_CONNECTION_RETRY, CHORD_CONNECTION_TIMEOUT));
            }
            catch (final Exception e) {

                Diagnostic.trace(DiagnosticLevel.FULL, "giving up establishing reference to: " + inet_socket_address);

                if (try_registry_on_connection_error) {
                    establishApplicationReferenceViaRegistry(host_descriptor, inet_socket_address);
                }
                else {
                    throw e;
                }
            }
        }
        finally {
            //            conc.decrementAndGet();
        }
    }

    private void establishApplicationReferenceViaRegistry(final HostDescriptor host_descriptor, final InetSocketAddress inet_socket_address) throws RegistryUnavailableException, RPCException {

        // Try accessing Chord via the registry.
        final InetAddress address = inet_socket_address.getAddress();
        final IRegistry registry = LocateRegistry.getRegistry(address);
        final int chord_port = registry.lookup(ChordRemoteServer.DEFAULT_REGISTRY_KEY);

        host_descriptor.applicationReference(factory.bindToNode(new InetSocketAddress(address, chord_port)));
        System.out.println("overwriting port " + host_descriptor.getPort() + " with " + chord_port);
        host_descriptor.port(chord_port);
    }

    @Override
    public void killApplication(final HostDescriptor host_descriptor) throws Exception {

        // Check whether a process handle exists, implying that the node was initiated by the current Java process.
        if (host_descriptor.getNumberOfProcesses() > 0) {
            super.killApplication(host_descriptor);
        }
        else {
            // If not, try to kill the process by guessing the format of the process name.
            final String host_name = stripLocalSuffix(host_descriptor.getInetAddress().getCanonicalHostName());
            final String match_fragment = NodeServer.class.getName() + " -s" + host_name + ":" + host_descriptor.getPort();
            host_descriptor.getProcessManager().killMatchingProcesses(match_fragment);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    protected P2PNodeFactory getP2PNodeFactory() {

        return factory;
    }

    // -------------------------------------------------------------------------------------------------------

    private String stripLocalSuffix(final String host_name) {

        return host_name.endsWith(LOCAL_HOSTNAME_SUFFIX) ? host_name.substring(0, host_name.length() - LOCAL_HOSTNAME_SUFFIX.length()) : host_name;
    }
}
