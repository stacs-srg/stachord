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
package uk.ac.standrews.cs.stachord.servers;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.impl.Constants;

/**
 * Provides the entry point for deploying a Chord node that creates a new Chord ring.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class StartNodeInNewRing extends AbstractServer {

	/**
	 * @param args
	 */
	public StartNodeInNewRing(String[] args) {
		
		super(args);
	}

	/**
	 * The following command line parameters are available:
	 * <dl>
	 * 	<dt>-s[host][:port] (required)</dt>
	 * 	<dd>Specifies the local address and port at which the Chord service should be made available.
	 * 		If no address is specified then the local loopback address (127.0.0.1) is used.
	 * 		If no port is specified then the default RMI port is used ({@link Constants#DEFAULT_RMI_REGISTRY_PORT}). </dd>
	 * 
	 *	<dt>-xkey (required)</dt>
	 *	<dd>Specifies the address and port for a known host that will be used to join the Chord ring
	 * 		If no address is specified then the local loopback address (127.0.0.1) is used.
	 * 		If no port is specified then the default RMI port is used ({@link Constants#DEFAULT_RMI_REGISTRY_PORT}). </dd>
	 * </dl>
	 * 
	 * @param args see above
	 * @throws RemoteException if an error occurs in making the new node accessible for remote access, or in communication with the remote machine
	 * @throws NotBoundException if the node in the existing ring is not accessible with the expected service name
	 */
	public static void main(String[] args) throws RemoteException, NotBoundException {

		StartNodeInNewRing starter = new StartNodeInNewRing(args);
		starter.createNode();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void createNode() throws RemoteException, NotBoundException {
		
		Diagnostic.traceNoSource(DiagnosticLevel.FULL, "Starting new RMI Chord ring with address: ", local_address, " on port: ", local_port, " with key: ", server_key);

		InetSocketAddress local_socket_address = new InetSocketAddress(local_address, local_port);

		if (server_key == null) ChordNodeFactory.createNode(local_socket_address);
		else                    ChordNodeFactory.createNode(local_socket_address, null, server_key);
	}

	protected void usage() {
			
		ErrorHandling.hardError( "Usage: -s[host][:port] [-xkey]" );
	}
}
