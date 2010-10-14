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

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.impl.Constants;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;

/**
 * Provides the entry point for deploying a Chord node that is joining an existing Chord ring.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class StartNodeInExistingRing extends AbstractServer {

    private final String known_address;
    private final int known_port;

    private StartNodeInExistingRing(final String[] args) {

        super(args);

        final String known_address_parameter = CommandLineArgs.getArg(args, "-k");
        if (known_address_parameter == null) {
            usage();
        }

        known_address = NetworkUtil.extractHostName(known_address_parameter);
        known_port = NetworkUtil.extractPortNumber(known_address_parameter);
    }

    /**
     * Creates a node that joins an existing ring.
     * The following command line parameters are available:
     * <dl>
     * 	<dt>-s[host][:port] (required)</dt>
     * 	<dd>Specifies the local address and port at which the Chord service should be made available.
     * 		If no address is specified then the local loopback address (127.0.0.1) is used.
     * 		If no port is specified then the default RMI port is used ({@link Constants#DEFAULT_RMI_REGISTRY_PORT}). </dd>
     *
     *	<dt>-k[host][:port] (required)</dt>
     *	<dd>Specifies the address and port for a known host that will be used to join the Chord ring
     * 		If no address is specified then the local loopback address (127.0.0.1) is used.
     * 		If no port is specified then the default RMI port is used ({@link Constants#DEFAULT_RMI_REGISTRY_PORT}). </dd>
     *
     *	<dt>-xkey (optional)</dt>
     *	<dd>Specifies the address and port for a known host that will be used to join the Chord ring
     * 		If no address is specified then the local loopback address (127.0.0.1) is used.
     * 		If no port is specified then the default RMI port is used ({@link Constants#DEFAULT_RMI_REGISTRY_PORT}). </dd>
     * </dl>
     *
     * @param args see above
     * @throws RemoteException if an error occurs in making the new node accessible for remote access, or in communication with the remote machine
     * @throws NotBoundException if the node in the existing ring is not accessible with the expected service name
     */
    public static void main(final String[] args) throws RemoteException, NotBoundException {

        final StartNodeInExistingRing starter = new StartNodeInExistingRing(args);
        starter.createNode();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void createNode() throws RemoteException, NotBoundException {

        Diagnostic.traceNoSource(DiagnosticLevel.FULL, "Joining RMI Chord ring with address: ", local_address, " on port: ", local_port, ", known node: ", known_address, " on port: ", known_port, " with key: ", server_key);

        final InetSocketAddress known_socket_address = new InetSocketAddress(known_address, known_port);

        final IChordNode node = makeNode();

        node.join(ChordNodeFactory.bindToRemoteNode(known_socket_address));
    }

    @Override
    protected void usage() {

        ErrorHandling.hardError("Usage: -s[host][:port] -k[host][:port] [-xkey]");
    }
}
