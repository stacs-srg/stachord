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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.exceptions.UnknownPlatformException;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.servers.StartNodeInNewRing;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Provides methods for creating new Chord nodes and binding to existing remote Chord nodes.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class ChordNodeFactory {

    // TODO extract generic code for binding with retry, and unify with TromboneNodeFactory.

    // TODO have target node select free port and notify back to temporary server.

    /**
     * Timeout interval for connection to remote nodes, in ms.
     */
    public static final int TIMEOUT_INTERVAL = 20000;

    /**
     * Timeout interval for establishing connection to a free port, in ms.
     */
    public static final int FREE_PORT_TIMEOUT_INTERVAL = 60000;

    private static final int RETRY_INTERVAL = 2000; // Retry connecting to remote nodes at 2s intervals.
    private static final int INITIAL_PORT = 54496;

    private static int next_port = INITIAL_PORT; // The next port to be used; static to allow multiple concurrent networks.
    private static final Object SYNC = new Object(); // Used for serializing network creation.

    // -------------------------------------------------------------------------------------------------------

    /**
     * Prevent instantiation of utility class.
     */
    private ChordNodeFactory() {

    }

    // -------------------------------------------------------------------------------------------------------

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
    public static IChordNode createNode(final InetSocketAddress local_address) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException {

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
    public static IChordNode createNode(final InetSocketAddress local_address, final IKey key) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException {

        return new ChordNodeImpl(local_address, key);
    }

    /**
     * Creates a new Chord node running in another JVM at a given network address on a given port, establishing a new one-node ring.
     *
     * @param host_descriptor a structure containing access details for a remote host
     * @return a remote reference to the new Chord node
     *
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     */
    public static IChordRemoteReference createNode(final HostDescriptor host_descriptor) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        return createNode(host_descriptor, null);
    }

    /**
     * Creates a new Chord node running in another JVM at a given network address on a given port, establishing a new one-node ring.
     *
     * @param host_descriptor a structure containing access details for a remote host
     * @param key the key of the new node
     * @return a remote reference to the new Chord node
     *
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     */
    public static IChordRemoteReference createNode(final HostDescriptor host_descriptor, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        instantiateNode(host_descriptor, key);

        return bindToNodeWithRetry(NetworkUtil.getInetSocketAddress(host_descriptor.getHost(), host_descriptor.getPort()));
    }

    /**
     * Creates a new Chord node running at a given remote network address on a given port, establishing a new one-node ring.
     *
     * @param host_descriptor a structure containing access details for a remote host
     * @return a process handle for the new node
     *
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     */
    public static void instantiateNode(final HostDescriptor host_descriptor) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        instantiateNode(host_descriptor, null);
    }

    /**
     * Creates a new Chord node running at a given network address on a given port, with a given key, establishing a new one-node ring.
     *
     * @param host_descriptor a structure containing access details for a remote host
     * @param key the key of the new node
     * @return a process handle for the new node
     *
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     */
    public static void instantiateNode(final HostDescriptor host_descriptor, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        // TODO unify these options so both set the application reference in the host descriptor before returning.
        final int port = host_descriptor.getPort();

        if (port == 0) {
            createAndBindToNodeOnFreePort(host_descriptor, key);
        }
        else {
            final List<String> args = constructArgs(host_descriptor, key, port);

            host_descriptor.process(host_descriptor.getProcessManager().runJavaProcess(StartNodeInNewRing.class, args));
        }
    }

    /**
     * Binds to an existing remote Chord node running at a given network address, checking for liveness.
     *
     * @param node_address the address of the existing node
     * @return a remote reference to the node
     *
     * @throws RPCException if an error occurs communicating with the remote machine
     */
    public static IChordRemoteReference bindToNode(final InetSocketAddress node_address) throws RPCException {

        final ChordRemoteReference remote_reference = new ChordRemoteReference(node_address);

        // Check that the remote application can be contacted.
        remote_reference.getRemote().isAlive();

        return remote_reference;
    }

    /**
     * Binds to an existing remote Chord node running at a given network address, retrying on any error until the timeout interval ({@link #TIMEOUT_INTERVAL}) has elapsed.
     *
     * @param node_address the address of the existing node
     * @return a remote reference to the node
     *
     * @throws TimeoutException if the node cannot be bound to within the timeout interval
     */
    public static IChordRemoteReference bindToNodeWithRetry(final InetSocketAddress node_address) throws TimeoutException {

        final long start_time = System.currentTimeMillis();

        while (true) {

            try {
                return bindToNode(node_address);
            }
            catch (final RPCException e) {
                Diagnostic.trace(DiagnosticLevel.FULL, "remote binding failed: " + e.getMessage());
            }

            try {
                Thread.sleep(RETRY_INTERVAL);
            }
            catch (final InterruptedException e) {
                // Ignore.
            }

            final long duration = System.currentTimeMillis() - start_time;
            if (duration > TIMEOUT_INTERVAL) { throw new TimeoutException(); }
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new Chord node running at a given network address on a free port, with a given key, establishing a new one-node ring.
     * The port already set in the host descriptor parameter, if any, is ignored. The host descriptor is updated with the selected port
     * and the process handle for the new node.
     *
     * @param host_descriptor a structure containing access details for a remote host
     * @param key the key of the new node
     *
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period ({@link #FREE_PORT_TIMEOUT_INTERVAL})
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     */
    private static void createAndBindToNodeOnFreePort(final HostDescriptor host_descriptor, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        final IArgGen arg_gen = new IArgGen() {

            @Override
            public List<String> getArgs(final int local_port) {

                return constructArgs(host_descriptor, key, local_port);
            }
        };

        final long start_time = System.currentTimeMillis();

        while (true) {

            int port = 0;

            synchronized (SYNC) {
                port = next_port++;
            }

            Diagnostic.trace(DiagnosticLevel.FULL, "trying to create node with port: " + port);

            host_descriptor.port(port);
            final List<String> args = arg_gen.getArgs(port);

            try {
                final Process chord_process = host_descriptor.getProcessManager().runJavaProcess(StartNodeInNewRing.class, args);
                host_descriptor.process(chord_process);

                final InetSocketAddress host_address = host_descriptor.getInetSocketAddress();
                final IChordRemoteReference chord_application_reference = bindToNodeWithRetry(host_address);

                host_descriptor.applicationReference(chord_application_reference);
                return;
            }
            catch (final TimeoutException e) {
                Diagnostic.trace("timed out trying to connect to port: " + port);
            }

            final long duration = System.currentTimeMillis() - start_time;
            if (duration > FREE_PORT_TIMEOUT_INTERVAL) { throw new TimeoutException(); }
        }
    }

    private static List<String> constructArgs(final HostDescriptor host_descriptor, final IKey key, final int port) {

        final List<String> args = new ArrayList<String>();

        args.add("-s" + NetworkUtil.formatHostAddress(host_descriptor.getHost(), port));
        args.add("-D" + Diagnostic.getLevel().numericalValue());

        if (key != null) {
            args.add("-x" + key.toString(Key.DEFAULT_RADIX));
        }

        return args;
    }

    private interface IArgGen {

        List<String> getArgs(int local_port);
    }
}
