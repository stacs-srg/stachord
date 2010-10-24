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
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.ProcessInvocation;
import uk.ac.standrews.cs.nds.remote_management.UnknownPlatformException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.servers.StartNodeInNewRing;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Provides methods for creating new Chord nodes and binding to existing remote Chord nodes.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public final class ChordNodeFactory {

    /**
     * Timeout interval for connection to remote nodes, in ms.
     */
    public static final int REGISTRY_TIMEOUT_INTERVAL = 20000;

    /**
     * Timeout interval for establishing connection to a free port, in ms.
     */
    public static final int FREE_PORT_TIMEOUT_INTERVAL = 60000;

    private static final int REGISTRY_RETRY_INTERVAL = 2000; // Retry connecting to remote nodes at 2s intervals.

    private static final int INITIAL_PORT = 54496;

    private static int next_port = INITIAL_PORT; // The next port to be used; static to allow multiple concurrent networks.

    private static final Object SYNC = new Object(); // Used for serializing network creation.

    /**
     * Prevent instantiation of utility class.
     */
    private ChordNodeFactory() {

    }

    /**
     * Creates a new Chord node running at a given local network address on a given port, establishing a new one-node ring.
     *
     * @param local_address the local address of the node
     * @return the new node
     * @throws RemoteException if an error occurs in making the new node accessible for remote access
     */
    public static IChordNode createLocalNode(final InetSocketAddress local_address) throws RemoteException {

        return new ChordNodeImpl(local_address);
    }

    /**
     * Creates a new Chord node running at a given local network address on a given port, with a given key, establishing a new one-node ring.
     *
     * @param local_address the local address of the node
     * @param key the key of the new node
     * @return the new node
     * @throws RemoteException if an error occurs in making the new node accessible for remote access
     */
    public static IChordNode createLocalNode(final InetSocketAddress local_address, final IKey key) throws RemoteException {

        return new ChordNodeImpl(local_address, key);
    }

    /**
     * Creates a new Chord node running at a given remote network address on a given port, establishing a new one-node ring.
     *
     * @param host_descriptor a structure containing access details for a remote host
     * @return a remote reference to the new Chord node
     *
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     */
    public static IChordRemoteReference createRemoteNode(final HostDescriptor host_descriptor) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        return createRemoteNode(host_descriptor, null);
    }

    /**
     * Creates a new Chord node running at a given remote network address on a given port, establishing a new one-node ring.
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
    public static IChordRemoteReference createRemoteNode(final HostDescriptor host_descriptor, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        instantiateRemoteNode(host_descriptor, key);
        return bindToRemoteNodeWithRetry(NetworkUtil.getInetSocketAddress(host_descriptor.getHost(), host_descriptor.getPort()));
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
    public static Process instantiateRemoteNode(final HostDescriptor host_descriptor) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        return instantiateRemoteNode(host_descriptor, null);
    }

    /**
     * Creates a new Chord node running at a given remote network address on a given port, with a given key, establishing a new one-node ring.
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
    public static Process instantiateRemoteNode(final HostDescriptor host_descriptor, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        final List<String> args = new ArrayList<String>();

        args.add("-s" + NetworkUtil.formatHostAddress(host_descriptor.getHost(), host_descriptor.getPort()));
        if (key != null) {
            addKeyArg(key, args);
        }

        return ProcessInvocation.runJavaProcess(StartNodeInNewRing.class, args, host_descriptor);
    }

    /**
     * Creates a new Chord node running at a given remote network address on a free port, with a given key, establishing a new one-node ring.
     * The port already set in the host_descriptor parameter, if any, is ignored
     *
     * @param host_descriptor a structure containing access details for a remote host
     * @param key the key of the new node
     *
     * @throws IOException if an error occurs when reading communicating with the remote host
     * @throws SSH2Exception if an SSH connection to the remote host cannot be established
     * @throws TimeoutException if the node cannot be instantiated within the timeout period ({@link #FREE_PORT_TIMEOUT_INTERVAL})
     * @throws UnknownPlatformException if the operating system of the remote host cannot be established
     */
    public static void createAndBindToRemoteNodeOnFreePort(final HostDescriptor host_descriptor, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        final IArgGen arg_gen = new IArgGen() {

            @Override
            public List<String> getArgs(final int local_port) {

                final List<String> args = new ArrayList<String>();

                args.add("-s" + NetworkUtil.formatHostAddress(host_descriptor.getHost(), local_port));
                addKeyArg(key, args);

                return args;
            }
        };

        createAndBindToRemoteNodeOnFreePort(host_descriptor, arg_gen, StartNodeInNewRing.class);
    }

    /**
     * Binds to an existing remote Chord node running at a given network address.
     *
     * @param node_address the address of the existing node
     * @return a remote reference to the node
     *
     * @throws RemoteException if an error occurs communicating with the remote machine
     * @throws NotBoundException if the Chord node is not accessible with the expected service name
     */
    public static IChordRemoteReference bindToRemoteNode(final InetSocketAddress node_address) throws RemoteException, NotBoundException {

        final Registry registry = LocateRegistry.getRegistry(node_address.getHostName(), node_address.getPort()); // This doesn't make a remote call.
        final IChordRemote node = (IChordRemote) registry.lookup(IChordRemote.CHORD_REMOTE_SERVICE_NAME);

        return new ChordRemoteReference(node.getKey(), node);
    }

    /**
     * Binds to an existing remote Chord node running at a given network address, retrying on any error until the timeout interval ({@link #REGISTRY_TIMEOUT_INTERVAL}) has elapsed.
     *
     * @param node_address the address of the existing node
     * @return a remote reference to the node
     *
     * @throws TimeoutException if the node cannot be bound to within the timeout interval
     */
    public static IChordRemoteReference bindToRemoteNodeWithRetry(final InetSocketAddress node_address) throws TimeoutException {

        final long start_time = System.currentTimeMillis();

        while (true) {

            try {
                return bindToRemoteNode(node_address);
            }
            catch (final RemoteException e) {
                Diagnostic.trace(DiagnosticLevel.FULL, "registry location failed: " + e.getMessage());
            }
            catch (final NotBoundException e) {
                Diagnostic.trace(DiagnosticLevel.FULL, "binding to node in registry failed");
            }
            catch (final Exception e) {
                Diagnostic.trace(DiagnosticLevel.FULL, "registry lookup failed");
            }

            try {
                Thread.sleep(REGISTRY_RETRY_INTERVAL);
            }
            catch (final InterruptedException e) {
            }

            final long duration = System.currentTimeMillis() - start_time;
            if (duration > REGISTRY_TIMEOUT_INTERVAL) { throw new TimeoutException(); }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static void createAndBindToRemoteNodeOnFreePort(final HostDescriptor host_descriptor, final IArgGen arg_gen, final Class<?> clazz) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

        final long start_time = System.currentTimeMillis();

        boolean finished = false;

        while (!finished) {

            int port = 0;

            synchronized (SYNC) {
                port = next_port++;
            }

            host_descriptor.setPort(port);

            final List<String> args = arg_gen.getArgs(port);

            try {
                host_descriptor.setProcess(ProcessInvocation.runJavaProcess(clazz, args, host_descriptor));
                host_descriptor.setApplicationReference(bindToRemoteNodeWithRetry(NetworkUtil.getInetSocketAddress(host_descriptor.getHost(), host_descriptor.getPort())));
                finished = true;
            }
            catch (final TimeoutException e) {
                Diagnostic.trace(DiagnosticLevel.FULL, "timed out trying to connect to port: " + port);
            }

            final long duration = System.currentTimeMillis() - start_time;
            if (duration > FREE_PORT_TIMEOUT_INTERVAL) { throw new TimeoutException(); }
        }
    }

    private static void addKeyArg(final IKey key, final List<String> args) {

        if (key != null) {
            args.add("-x" + key.toString(Key.DEFAULT_RADIX));
        }
    }

    private interface IArgGen {

        List<String> getArgs(int local_port);
    }
}
