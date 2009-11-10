package uk.ac.standrews.cs.stachordRMI.servers;

/*
 *  ASA Library
 *  Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 *  http://asa.cs.st-andrews.ac.uk/
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * Created on 03-Nov-2004
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachordRMI.deploy.ChordRMIDeployment;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;

/**
 * Creates a Chord node that creates a new ring or joins an existing ring and
 * sends state change Events to a ChordVisualiser for ring visualisation.
 *
 * Four command line parameters are accepted:
 * <dl>
 *	<dt>-k[host][:port]</dt>
 *	<dd>Specifies the hostname/ip address for a host that will be used to
 * 		join the Chord ring and the port on which the Chord service should
 * 		be contacted. If the '-k' parameter is omitted the node on the
 * 		local host creates a new ring. Note: if no host
 * 		is specified i.e. "-k", "-k:" or "-k:12345" then then the local
 * 		loopback address (127.0.01) is used. If a host is specified, but no port, then this
 * 		node will try to contact the remote known node on the default port, 52525. </dd>
 * 	<dt>-s[host][:port]</dt>
 * 	<dd>Specifies the host address and port (associated with a particular interface) for
 * 		the local machine on which the Chord services should be made available.
 * 	</dd>
 * </dl>
 *
 * @author stuart, graham
 */
public class ChordServer {

	private static final DiagnosticLevel DEFAULT_DIAGNOSTIC_LEVEL = DiagnosticLevel.FULL;

	private static final String RMI_POLICY_FILENAME = "rmiPolicy";
	
	public static void main(String[] args) throws P2PNodeException {
		//this may be overridden by a CLA
		Diagnostic.setLevel(DEFAULT_DIAGNOSTIC_LEVEL);
		//Diagnostic.setLevel(DiagnosticLevel.FULL);
		Diagnostic.setTimestampFlag(true);
		Diagnostic.setTimestampFormat(new SimpleDateFormat("HH:mm:ss:SSS "));
		Diagnostic.setTimestampDelimiterFlag(false);
		ErrorHandling.setTimestampFlag(false);

		// RMI Policy runes from Ben 
		
		System.setProperty("java.security.policy", RMI_POLICY_FILENAME); 
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}
		
		InetSocketAddress local_node_address = null;
		String server_address_parameter = null;
		try {
			server_address_parameter = CommandLineArgs.getArg(args, "-s"); // This nodes's address
			if (server_address_parameter != null) {
				String addr = NetworkUtil.extractHostName(server_address_parameter);
				InetAddress local_address = InetAddress.getByName(addr);
				int local_port = NetworkUtil.extractPortNumber(server_address_parameter);
				local_node_address = new InetSocketAddress( local_address,local_port );
			}else {
				ErrorHandling.hardError( "No local address specified - use -s option" );
			}
		} catch( Exception e ) {
			ErrorHandling.hardError( "Exception in establishing local socket addresses" + server_address_parameter );
		}
		InetSocketAddress known_node_address = null;
		String known_address_parameter = null;
		try {
			known_address_parameter = CommandLineArgs.getArg(args, "-k"); // Known ChordNode
			if (known_address_parameter != null) {
				String addr = NetworkUtil.extractHostName(known_address_parameter);
				InetAddress known_address = InetAddress.getByName(addr);
				int known_port = NetworkUtil.extractPortNumber(known_address_parameter);
				known_node_address = new InetSocketAddress( known_address,known_port );
			} else {
				known_address_parameter = "unspecified";
			}
		} catch( Exception e ) {
			ErrorHandling.hardError( "Exception in establishing known node socket addresses" + known_address_parameter );
		}
		Diagnostic.traceNoSource(DiagnosticLevel.RUN, "Starting RMI Chord node with local address: " + server_address_parameter + " and known node address: " + known_address_parameter );
		try {
			IChordNode node = ChordRMIDeployment.deployNode( local_node_address, known_node_address );
		} catch (RemoteException e) {
			ErrorHandling.hardError( "Exception in initialising node: node already initialised? (potential key/address reuse" );
		}

	}


}
