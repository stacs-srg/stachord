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
/*
 * Created on 02-Nov-2004
 */
package uk.ac.standrews.cs.stachordRMI.deploy;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.eventModel.IEventGenerator;
import uk.ac.standrews.cs.nds.eventModel.eventBus.busInterfaces.IEventBus;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.P2PStatus;
import uk.ac.standrews.cs.nds.p2p.interfaces.IApplicationRegistry;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.HashBasedKeyFactory;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

/**
 * Provides a number of static methods for creating a ChordNode. No maintenance thread is created.
 *
 * @author stuart, graham
 */
public class SingleJVMChordDeployment {

	public static final P2PStatus initialisationSuccess = P2PStatus.NODE_RUNNING;

	private static HashBasedKeyFactory key_factory = new SHA1KeyFactory();

	/**
	 * Deploys a Chord node configured to join an existing ring.
	 *
	 * @param local_node the address and port and key for the local Chord node
	 * @param local_node_key the key for the local Chord node
	 * @param known_node a node in an existing ring that will be joined by the local node
	 * @param bus a bus object through which the Chord node will publish events
	 * @param appRegistry an application registry
	 * @return the deployed node
	 * @throws P2PNodeException if an error occurs during node initialisation or joining the existing network.
	 */
	public static IChordNode customDeployment(InetSocketAddress local_node, IKey local_node_key, IChordRemoteReference known_node, IEventBus bus, IApplicationRegistry appRegistry) throws RemoteException {

		IChordNode node = nodeDeployment(local_node, local_node_key, known_node, bus, appRegistry);

		return node;
	}

	/**
	 * Deploys a Chord node configured to create a new ring.
	 *
	 * @param local_node the address and port and key for the local Chord node
	 * @param local_node_key the key for the local Chord node
	 * @param bus a bus object through which the Chord node will publish events
	 * @param appRegistry an application registry
	 * @return the deployed node
	 * @throws P2PNodeException if an error occurs during node initialisation or joining the existing network.
	 */
	public static IChordNode customDeployment(InetSocketAddress local_node, IKey local_node_key, IEventBus bus, IApplicationRegistry appRegistry) throws RemoteException {

		IChordNode node = nodeDeployment(local_node, local_node_key, null, bus, appRegistry);

		return node;
	}

	public static IChordNode nodeDeployment(InetSocketAddress local_node, IKey local_node_key, IChordRemoteReference known_node, IEventBus bus, IApplicationRegistry appRegistry) throws RemoteException {

		IKey node_key = local_node_key==null?key_factory.generateKey(local_node):local_node_key;
		IChordNode node = new ChordNodeImpl(local_node, node_key, bus, appRegistry);

		if (known_node == null) node.createRing();
		else node.join(known_node);

		return node;
	}

	/*Addition 3-2-08 Markus Tauber*/
	public static IChordNode customDeployment(InetSocketAddress local_node, IKey localKey,
			IChordRemoteReference knownNode, IEventBus bus, IApplicationRegistry applicationRegistry,
			IEventGenerator eventGenerator) throws RemoteException {

		IKey node_key = localKey == null ? key_factory.generateKey(local_node) : localKey;
		IChordNode node = new ChordNodeImpl(local_node, node_key, bus, applicationRegistry, eventGenerator);

		if (knownNode == null) node.createRing();
		else node.join(knownNode);

		return node;
	}
	/*Addition end */
}