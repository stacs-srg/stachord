/*
 *  StAChord Library
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
package uk.ac.standrews.cs.stachordRMI.deploy;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import uk.ac.standrews.cs.nds.eventModel.eventBus.EventBus;
import uk.ac.standrews.cs.nds.eventModel.eventBus.busInterfaces.IEventBus;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.P2PStatus;
import uk.ac.standrews.cs.nds.p2p.interfaces.IApplicationRegistry;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.HashBasedKeyFactory;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.impl.ApplicationRegistryImpl;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.impl.ChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.impl.CustomSocketFactory;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

/**
 * Provides a number of static methods for deploying a Chord node.
 * In all cases the ChordSingleton factory class is used to instantiate the Chord node.
 *
 * @author stuart, graham, al (RMI)
 */
public class ChordRMIDeployment {

	/**
	 * Name under which the Chord service is exposed.
	 */
	public static final String CHORD_REMOTE_SERVICE = IChordRemote.class.getSimpleName();

	public static final P2PStatus initialisationSuccess = P2PStatus.NODE_RUNNING;

	/**
	 * The default port for the Chord node RRT. Different from
	 * RemoteRRTRegistry default port to allow admin node and a single default
	 * Chord node to coexist.
	 */

	private static HashBasedKeyFactory key_factory = new SHA1KeyFactory();


	private static IChordNode initialise(InetSocketAddress node_rep, IKey key, IChordRemoteReference known_node, IEventBus bus, IApplicationRegistry registry  ) throws RemoteException, P2PNodeException {

		IChordNode instance = new ChordNodeImpl( node_rep, key, bus, registry );

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
		
		IApplicationRegistry registry = new ApplicationRegistryImpl();
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
		
		node =  initialise(local_node_address, node_key, known_node_remote_ref, bus, registry );
		
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
}