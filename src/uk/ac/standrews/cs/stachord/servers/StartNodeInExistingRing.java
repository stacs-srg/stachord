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

import java.io.IOException;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.UndefinedDiagnosticLevelException;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;

/**
 * Provides the entry point for deploying a Chord node that is joining an existing Chord ring.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class StartNodeInExistingRing extends AbstractServer {

    private final String known_address;
    private final int known_port;

    private StartNodeInExistingRing(final String[] args) throws UndefinedDiagnosticLevelException {

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
     * <dt>-shost:port (required)</dt>
     * <dd>Specifies the local address and port at which the Chord service should be made available.</dd>
     *
     * <dt>-khost:port (required)</dt>
     * <dd>Specifies the address and port for a known host that will be used to join the Chord ring. </dd>
     *
     * <dt>-xkey (optional)</dt>
     * <dd>Specifies the key for the new Chord node.</dd>
     *
     * <dt>-Dlevel (optional)</dt>
     * <dd>Specifies a diagnostic level from 0 (most detailed) to 6 (least detailed).</dd>
     * </dl>
     *
     * @param args see above
     * @throws RPCException if an error occurs in making the new node accessible for remote access, or in communication with the remote machine
     * @throws UndefinedDiagnosticLevelException if the specified diagnostic level is not valid
     * @throws IOException if a node cannot be created using the given local address
     * @throws RPCException if an error occurs binding the node to the registry
     * @throws AlreadyBoundException if another node is already bound in the registry
     * @throws RegistryUnavailableException if the registry is unavailable
     */
    public static void main(final String[] args) throws RPCException, UndefinedDiagnosticLevelException, IOException, AlreadyBoundException, RegistryUnavailableException {

        final StartNodeInExistingRing starter = new StartNodeInExistingRing(args);
        starter.createNode();
    }

    // -------------------------------------------------------------------------------------------------------

    private void createNode() throws RPCException, IOException, AlreadyBoundException, RegistryUnavailableException {

        Diagnostic.traceNoSource(DiagnosticLevel.FULL, "Joining Chord ring with address: ", local_address, " on port: ", local_port, ", known node: ", known_address, " on port: ", known_port, " with key: ", server_key);

        final InetSocketAddress known_socket_address = new InetSocketAddress(known_address, known_port);

        final IChordNode node = makeNode();

        node.join(new ChordNodeFactory().bindToNode(known_socket_address));
    }

    @Override
    protected void usage() {

        ErrorHandling.hardError("Usage: -shost:port -k[host][:port] [-xkey] [-Dlevel]");
    }
}
