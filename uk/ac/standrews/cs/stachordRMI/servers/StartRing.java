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

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;

/**
 * Provides the entry point for deploying a Chord node that creates a new Chord Ring
 *
 * Creates a Chord node that creates a new ring.
 *
 * A single command line parameter is required:
 * <dl>
 * 	<dt>-s[host][:port]</dt>
 * 	<dd>Specifies the host address and port for the local machine on which the Chord services should be made available.
 * 	</dd>
 * </dl>
 * 
 * @author al
 */
public class StartRing extends AbstractServer {

	public static void main(String[] args) {

		setup(args);

		Diagnostic.trace(DiagnosticLevel.FULL, "Starting new RMI Chord ring with address: ", local_address, " on port: ", local_port, " with key: ", server_key);

		InetSocketAddress local_socket_address = new InetSocketAddress(local_address, local_port);

		try {
			if (server_key == null) new ChordNodeImpl(local_socket_address, null);
			else                    new ChordNodeImpl(local_socket_address, null, server_key);
		}
		catch (Exception e) {
			Diagnostic.trace(DiagnosticLevel.FULL, "Failed to start new RMI Chord ring: " + e.getMessage());
		}
	}
}
