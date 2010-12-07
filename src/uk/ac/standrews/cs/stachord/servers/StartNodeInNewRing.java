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

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.UndefinedDiagnosticLevelException;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;

/**
 * Provides the entry point for deploying a Chord node in a new Chord ring.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public final class StartNodeInNewRing extends AbstractServer {

    private StartNodeInNewRing(final String[] args) throws UndefinedDiagnosticLevelException {

        super(args);
    }

    /**
     * The following command line parameters are available:
     * <dl>
     * <dt>-s[host][:port] (required)</dt>
     * <dd>Specifies the local address and port at which the Chord service should be made available.
     * If no address is specified then the local loopback address (127.0.0.1) is used.
     * If no port is specified then the default RMI port is used ({@link IChordNode#DEFAULT_RMI_REGISTRY_PORT}). </dd>
     *
     * <dt>-xkey (optional)</dt>
     * <dd>Specifies the address and port for a known host that will be used to join the Chord ring
     * If no address is specified then the local loopback address (127.0.0.1) is used.
     * If no port is specified then the default RMI port is used ({@link IChordNode#DEFAULT_RMI_REGISTRY_PORT}). </dd>
     * 
     * <dt>-Dlevel (optional)</dt>
     * <dd>Specifies a diagnostic level from 0 (most detailed) to 6 (least detailed).</dd>
     * </dl>
     *
     * @param args see above
     * @throws RemoteException if an error occurs in making the new node accessible for remote access, or in communication with the remote machine
     * @throws NotBoundException if the node in the existing ring is not accessible with the expected service name
     * @throws UndefinedDiagnosticLevelException if the specified diagnostic level is not valid
     */
    public static void main(final String[] args) throws RemoteException, NotBoundException, UndefinedDiagnosticLevelException {

        final StartNodeInNewRing starter = new StartNodeInNewRing(args);
        starter.createNode();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void createNode() throws RemoteException {

        Diagnostic.traceNoSource(DiagnosticLevel.FULL, "Starting new RMI Chord ring with address: ", local_address, " on port: ", local_port, " with key: ", server_key);

        makeNode();
    }

    @Override
    protected void usage() {

        ErrorHandling.hardError("Usage: -s[host][:port] [-xkey] [-Dlevel]");
    }
}
