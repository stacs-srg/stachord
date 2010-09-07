/*******************************************************************************
 * StAChord Library
 * Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 * http://asa.cs.st-andrews.ac.uk/
 * 
 * This file is part of stachordRMI.
 * 
 * stachordRMI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * stachordRMI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with stachordRMI.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.standrews.cs.stachordRMI.servers;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;

/**
 * Provides the entry point for deploying a Chord node that is joining a Chord Ring
 *
 * * Creates a Chord node that joins an existing ring.
 *
 * Two command line parameters are required:
 * <dl>
 *	<dt>-k[host][:port]</dt>
 *	<dd>Specifies the hostname/ip address,port for a known host that will be used to join the Chord ring
 * 		Note: if no host is specified i.e. "-k", "-k:" or "-k:12345" then then the local loopback address (127.0.0.1) is used.
 * 		If a host is specified, but no port, then this node will try to contact the remote known node on the default port, 52525. </dd>
 * 	<dt>-s[host][:port]</dt>
 * 	<dd>Specifies the host address and port for the local machine on which the Chord services should be made available.
 * 	</dd>
 * </dl>
 * 
 * @author al
 */
public class StartNode extends AbstractServer {

	public static void main(String[] args) throws RemoteException, NotBoundException {
		
		setup(args);
		
		String known_address_parameter = CommandLineArgs.getArg(args, "-k"); // Known ChordNode
		if (known_address_parameter == null) usage();

		String known_address = NetworkUtil.extractHostName(known_address_parameter);
		int known_port =       NetworkUtil.extractPortNumber(known_address_parameter);

		Diagnostic.traceNoSource(DiagnosticLevel.FULL, "Joining RMI Chord ring with address: ", local_address, " on port: ", local_port, ", known node: ", known_address, " on port: ", known_port, " with key: ", server_key);

		InetSocketAddress local_socket_address = new InetSocketAddress(local_address, local_port);
		InetSocketAddress known_socket_address = new InetSocketAddress(known_address, known_port);
		
		if (server_key == null) new ChordNodeImpl(local_socket_address, known_socket_address);
		else                    new ChordNodeImpl(local_socket_address, known_socket_address, server_key);
	}

	private static void usage() {
			
		ErrorHandling.hardError( "Usage: -k[host][:port]" );
	}
}
