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
package uk.ac.standrews.cs.stachordRMI.testharness.impl;

import static org.junit.Assert.assertNotNull;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.interfaces.IGatewayLocator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PNode;
import uk.ac.standrews.cs.nds.p2p.interfaces.IP2PRouter;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetworkNodeHandle;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetworkWithGateway;

public abstract class AbstractNetworkWithGateway<T> extends AbstractChordNetwork<T> implements IChordNetworkWithGateway<T> {
	protected IGatewayLocator gl;

	public AbstractNetworkWithGateway(int n) throws Exception {
		super(n);
	}

	public AbstractNetworkWithGateway(String[] keyStrings) throws Exception {
		super(keyStrings);
	}

	public IGatewayLocator getGatewayLocator() throws Exception {
		return gl;
	}

	/**
	 * This method will return when all nodes in the network have registered with the gateway and the network's routing
	 * mesh has been fully constructed. It is assumed that the underlying overlay network implements the Chord
	 * protocol. The number of nodes in the network is explicitly stated with the parameter 'n'. The network is
	 * created using the specified INetworkFactory implementation. All nodes have registered when a call to findAll()
	 * in the IGatewayLocator implementation returned by the factory method contains 'n' nodes. The routing mesh has
	 * been fully constructed when a ring containing 'n' nodes has been formed and that ring can be traversed by
	 * following the predecessor and successor pointer. Note that this method is Chord specific.
	 */
	@Override
	public void waitForStableNetwork() {
		int n = nodes.size();
		//wait for all expected nodes to register
		loopUntilAllGatewaysRegistered();

		IKey[] keys=new IKey[n];
		int index=0;
		for(IChordNetworkNodeHandle<?> node:nodes) {
			keys[index++]=node.getChordNode().getKey();
		}
		// Wait for the routing mesh to be fully established.
		pollNetwork_AllHostKeysFromAllGateways(keys);
	}

	/**
	 * When this method returns a call to gl.findAll() will return the expected number of nodes for the network.
	 *
	 * @throws Exception
	 */
	private void loopUntilAllGatewaysRegistered() {

		IP2PRouter[] gateways = gl.getAllRegisteredGateways();
		while (gateways.length != nodes.size()) {
			gateways = gl.getAllRegisteredGateways();
		}
	}

	/**
	 * This method will return when all nodes in the network have registered with the gateway and the network's routing
	 * mesh has been fully constructed. The number of expected nodes in the network is implicitly the number of
	 * elements in the 'keys' array. All nodes have registered when a call to getAllRegisteredGateways() in the IGatewayLocator
	 * implementation 'gl' returns N nodes where N == keys.length. The routing mesh has been fully constructed when for
	 * each key value K represented in the keys array, the key K key routes to the node whose key in K.
	 *
	 * @param keys an array of IKey implementations with one element corresponding the key for each network node. The
	 *            expected size of the network is keys.length so there must be one key for each node in the network.

	 */
	private void pollNetwork_AllHostKeysFromAllGateways(IKey[] keys) {

		IP2PRouter[] gateways = gl.getAllRegisteredGateways();

		for(IP2PRouter gateway:gateways) {

			for(IKey k:keys){
				IKey nodeKey=null;
				do{
					IP2PNode p2pnode;
					try {
						p2pnode = gateway.lookup(k);
					} catch (P2PNodeException e) {
						continue;
					}
					nodeKey=p2pnode.getKey();
					assertNotNull(nodeKey);
				}
				while(nodeKey==null || !k.equals(nodeKey));
			}
		}
	}

	@Override
	public void killNetwork() {

		super.killNetwork();
		gl=null;
	}
}
