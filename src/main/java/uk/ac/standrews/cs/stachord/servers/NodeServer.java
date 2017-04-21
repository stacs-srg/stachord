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

package uk.ac.standrews.cs.stachord.servers;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.utilities.archive.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides the entry point for deploying a Chord node.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
public final class NodeServer {

    /** The key of the property that contains the actual address on which a constructed node is exposed. */
    public static final String ADDRESS_PROPERTY_KEY = "CHORD_NODE_LOCAL_ADDRESS";
    /** The key of the property that contains the process ID of a deployed process. */
    public static final String PID_PROPERTY_KEY = "pid";
    private static final Duration CHORD_SOCKET_READ_TIMEOUT = new Duration(20, TimeUnit.SECONDS);
    private static final ChordNodeFactory factory;
    private static final String NULL = "null";
    private static final char AT_SIGN = '@';
    private final Properties properties;
    private IKey node_key;
    private InetSocketAddress local_address;
    private InetSocketAddress join_address;

    static {
        factory = new ChordNodeFactory();
        StreamProxy.CONNECTION_POOL.setSocketReadTimeout(CHORD_SOCKET_READ_TIMEOUT);
    }

    public NodeServer() {

        properties = new Properties();
        setProperty(PID_PROPERTY_KEY, getPIDFromRuntimeMXBeanName());
    }

    /**
     * The following command line parameters are available:
     * <dl>
     * <dt>-shost:port (required)</dt>
     * <dd>Specifies the local address and port at which the Chord service should be made available.</dd>
     * <dt>-khost:port (optional)</dt>
     * <dd>Specifies the address and port of an existing Chord node, via which the new node should join the ring.</dd>
     * <dt>-xkey (optional)</dt>
     * <dd>Specifies the key for the new Chord node.</dd>
     * <dt>-Dlevel (optional)</dt>
     * <dd>Specifies a diagnostic level from 0 (most detailed) to 6 (least detailed).</dd>
     * </dl>
     *
     * @param args see above
     * @throws RPCException if an error occurs in making the new node accessible for remote access, or in communication with the remote machine
     * @throws UndefinedDiagnosticLevelException if the specified diagnostic level is not valid
     * @throws IOException if a node cannot be created using the given local address
     * @throws AlreadyBoundException if another node is already bound in the registry
     * @throws RegistryUnavailableException if the registry is unavailable
     * @throws TimeoutException
     * @throws InterruptedException
     */
    public static void main(final String[] args) throws RPCException, UndefinedDiagnosticLevelException, IOException, AlreadyBoundException, RegistryUnavailableException, InterruptedException, TimeoutException {

        final NodeServer server = new NodeServer();
        server.deploy(args);
        server.printProperties();
    }

    public IChordNode createNode() throws InterruptedException, RPCException, IOException, AlreadyBoundException, TimeoutException, RegistryUnavailableException {

        final IChordNode node = node_key == null ? factory.createNode(local_address) : factory.createNode(local_address, node_key);

        if (join_address != null) {
            node.join(factory.bindToNode(join_address));
        }

        return node;
    }

    private void printProperties() {

        System.out.println(NodeServer.class.getName() + properties);

    }

    private void setProperty(String key, Object value) {

        properties.setProperty(key, String.valueOf(value));
    }

    private static Integer getPIDFromRuntimeMXBeanName() {

        final String runtime_mxbean_name = ManagementFactory.getRuntimeMXBean().getName();
        final int index_of_at = runtime_mxbean_name.indexOf(AT_SIGN);
        return index_of_at != -1 ? Integer.parseInt(runtime_mxbean_name.substring(0, index_of_at)) : null;
    }

    private void configure(final String... args) throws UndefinedDiagnosticLevelException, UnknownHostException {

        final Map<String, String> arguments = CommandLineArgs.parseCommandLineArgs(args);

        configureDiagnostics(arguments);
        configureLocalAddress(arguments);
        configureJoinAddress(arguments);
        configureNodeKey(arguments);
    }

    protected void deploy(final String... args) throws UndefinedDiagnosticLevelException, UnknownHostException, InterruptedException, RegistryUnavailableException, RPCException, AlreadyBoundException, TimeoutException {

        configure(args);
        try {
            final IChordNode node = createNode();
            final InetSocketAddress node_address = node.getAddress();
            setProperty(ADDRESS_PROPERTY_KEY, node_address);
            Diagnostic.trace("Started Chord node at " + node_address);
        }
        catch (final IOException e) {
            Diagnostic.trace("Couldn't start Chord node at " + local_address + " : " + e.getMessage());
        }
    }

    private void usage() {

        ErrorHandling.hardError("Usage: -shost:port [-khost:port] [-xkey] [-Dlevel]");
    }

    private void configureDiagnostics(final Map<String, String> arguments) throws UndefinedDiagnosticLevelException {

        ErrorHandling.setTimestampFlag(false);
    }

    private void configureLocalAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String local_address_parameter = arguments.get("-s"); // This node's address.
        if (local_address_parameter == null) {
            usage();
        }
        local_address = NetworkUtil.extractInetSocketAddress(local_address_parameter, 0);
    }

    private void configureJoinAddress(final Map<String, String> arguments) throws UnknownHostException {

        final String known_address_parameter = arguments.get("-k");
        if (known_address_parameter != null) {
            join_address = NetworkUtil.extractInetSocketAddress(known_address_parameter, 0);
        }
    }

    private void configureNodeKey(final Map<String, String> arguments) {

        final String server_key_parameter = arguments.get("-x"); // This node's key.
        if (server_key_parameter != null && !server_key_parameter.equals(NULL)) {
            node_key = new Key(server_key_parameter);
        }
    }
}
