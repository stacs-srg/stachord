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
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Map;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.UndefinedDiagnosticLevelException;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;

/**
 * Common setup for {@link StartNodeInExistingRing} and {@link StartNodeInNewRing}.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
abstract class AbstractServer {

    private static final DiagnosticLevel DEFAULT_DIAGNOSTIC_LEVEL = DiagnosticLevel.NONE;

    protected String local_address;
    protected int local_port;
    protected IKey server_key;

    /**
     * Processes command-line arguments.
     * @param args the arguments
     * @throws UndefinedDiagnosticLevelException 
     */
    public AbstractServer(final String[] args) throws UndefinedDiagnosticLevelException {

        Diagnostic.setLevel(DEFAULT_DIAGNOSTIC_LEVEL);

        Diagnostic.setTimestampFlag(true);
        Diagnostic.setTimestampFormat(new SimpleDateFormat("HH:mm:ss:SSS "));
        Diagnostic.setTimestampDelimiterFlag(false);
        ErrorHandling.setTimestampFlag(false);

        final Map<String, String> arguments = CommandLineArgs.parseCommandLineArgs(args);

        configureDiagnostics(arguments);

        final String server_address_parameter = arguments.get("-s"); // This node's address.
        if (server_address_parameter == null) {
            usage();
        }

        local_address = NetworkUtil.extractHostName(server_address_parameter);
        local_port = NetworkUtil.extractPortNumber(server_address_parameter);

        final String server_key_parameter = CommandLineArgs.getArg(args, "-x"); // This node's key.
        if (server_key_parameter != null && !server_key_parameter.equals("null")) {
            server_key = new Key(server_key_parameter);
        }
    }

    protected IChordNode makeNode() throws RemoteException {

        final InetSocketAddress local_socket_address = new InetSocketAddress(local_address, local_port);
        return server_key == null ? ChordNodeFactory.createLocalNode(local_socket_address) : ChordNodeFactory.createLocalNode(local_socket_address, server_key);
    }

    protected abstract void usage();

    private static void configureDiagnostics(final Map<String, String> arguments) throws UndefinedDiagnosticLevelException {

        Diagnostic.setLevel(DiagnosticLevel.getDiagnosticLevelFromCommandLineArg(arguments.get("-D"), DEFAULT_DIAGNOSTIC_LEVEL));
    }
}
