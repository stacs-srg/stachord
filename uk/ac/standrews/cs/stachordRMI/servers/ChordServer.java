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
 * Created on 03-Nov-2004 - Modified extensively 11-Nov-2009 !!!!!
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;

import uk.ac.standrews.cs.nds.eventModel.eventBus.EventBus;
import uk.ac.standrews.cs.nds.eventModel.eventBus.busInterfaces.IEventBus;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.P2PStatus;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.HashBasedKeyFactory;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.CommandLineArgs;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachordRMI.factories.CustomSocketFactory;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.impl.ChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.impl.DefaultMaintenanceThread;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

/**
 * Provides the entry point for deploying a Chord node.
 *
 * * Creates a Chord node that creates a new ring or joins an existing ring and
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


 * @author stuart, graham, al (RMI)
 */
public class ChordServer {

	/**
	 * Name under which the Chord service is exposed.
	 */
	public static final String CHORD_REMOTE_SERVICE = IChordRemote.class.getSimpleName();

	public static final P2PStatus initialisationSuccess = P2PStatus.NODE_RUNNING;

	private static final DiagnosticLevel DEFAULT_DIAGNOSTIC_LEVEL = DiagnosticLevel.FULL;

	private static final String RMI_POLICY_FILENAME = "rmiPolicy";

	private static HashBasedKeyFactory key_factory = new SHA1KeyFactory();

	private static IChordNode initialise(InetSocketAddress node_rep, IKey key, IChordRemoteReference known_node, IEventBus bus  ) throws RemoteException, P2PNodeException {

		IChordNode instance = new ChordNodeImpl( node_rep, key, bus );

		if (known_node == null) {
			Diagnostic.trace( DiagnosticLevel.RUN, "Creating a new ring" );
			instance.createRing();
		} else {
			Diagnostic.trace( DiagnosticLevel.RUN, "Joining ring" );
			instance.join(known_node);
		}
		return instance;
	}
	
	public static IChordNode deployNode(InetSocketAddress local_node_address, InetSocketAddress known_node_address ) throws P2PNodeException, RemoteException {

		IChordRemoteReference known_node_remote_ref = null;
		
		IEventBus bus = new EventBus();
		IKey node_key = key_factory.generateKey(local_node_address);
		Diagnostic.trace( DiagnosticLevel.RUN, "Node Key: " + node_key );
		IChordNode node;
		
		// Setup/join the ring
		
		if( known_node_address != null ) {
			try {
					Diagnostic.trace( DiagnosticLevel.RUN, "Lookupup RMI Chord node at address: " + known_node_address.getHostName()  + ":" + known_node_address.getPort() );
					IChordRemote known_node_remote = (IChordRemote) LocateRegistry.getRegistry( known_node_address.getHostName(), known_node_address.getPort() ).lookup( CHORD_REMOTE_SERVICE );
					known_node_remote_ref = new ChordRemoteReference( known_node_remote.getKey(), known_node_remote );
			}
			catch (Exception e) {
					throw new RuntimeException( "Serialization error. Path to bad object: ");
					// throw new P2PNodeException(P2PStatus.KNOWN_NODE_FAILURE);
			}

		}
		
		node =  initialise(local_node_address, node_key, known_node_remote_ref, bus  );
		
		// Start maintenance thread
		Thread thread_for_maintenance = new DefaultMaintenanceThread(node);
		thread_for_maintenance.start();

		
		// Now start RMI listening
		try {
			IChordRemote stub = (IChordRemote) UnicastRemoteObject.exportObject(node.getProxy().getRemote(), 0); // NOTE the remote of the proxy is actually local!
		} catch (RemoteException e1) {
			throw new RuntimeException( "Cannot export object ", e1);
		}
		
		// Register the service with the registry
		
		Registry local_registry = null;
		try {
				// Obtains a stub for a registry on the local host on the default registry port
				// first parameter is the port where the RMI registry is listening
				// last paramter is the address where the service is going to be found.
				local_registry = LocateRegistry.createRegistry( local_node_address.getPort(), null, new CustomSocketFactory( local_node_address.getAddress() ) );
				Diagnostic.trace( DiagnosticLevel.RUN, "Local Registry deployed at:" + local_node_address.getAddress() + ":" + local_node_address.getPort() );
		}
		catch (Exception e) {
				throw new P2PNodeException(P2PStatus.SERVICE_DEPLOYMENT_FAILURE, "could not deploy \"" + IChordRemote.class.getName() + "\" interface due to registry failure");
		}
		try {
			local_registry.rebind( CHORD_REMOTE_SERVICE, node.getProxy().getRemote() );
			Diagnostic.trace( DiagnosticLevel.RUN, "Deployed RMI Chord node in local Registry [" + node + "]" );
		} catch (Exception e) {
			throw new P2PNodeException(P2PStatus.SERVICE_DEPLOYMENT_FAILURE, "could not deploy \"" + IChordRemote.class.getName() + "\" interface due to registry binding exception");
		}
		return node;
	}
	
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
			IChordNode node = deployNode( local_node_address, known_node_address );
		} catch (RemoteException e) {
			ErrorHandling.hardError( "Exception in initialising node: node already initialised? (potential key/address reuse" );
		}

	}


}
