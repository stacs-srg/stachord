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
package uk.ac.standrews.cs.stachordRMI.testharness.util;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.HashBasedKeyFactory;
import uk.ac.standrews.cs.nds.p2p.util.SHA1KeyFactory;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;

public class TestUtils {

	public static final String[] CLOCK_QUADRANT_KEYS_OFFSET_1 = { "0000000000000000000000000000000000000001", "4000000000000000000000000000000000000001",
		"8000000000000000000000000000000000000001", "c000000000000000000000000000000000000001" };

	public static final String[] CLOCK_QUADRANT_KEYS_OFFSET_1_PLUS_6XXX1 = { CLOCK_QUADRANT_KEYS_OFFSET_1[0], CLOCK_QUADRANT_KEYS_OFFSET_1[1],
		"6000000000000000000000000000000000000001", CLOCK_QUADRANT_KEYS_OFFSET_1[2], CLOCK_QUADRANT_KEYS_OFFSET_1[3] };

	public static final StorageSpec[] DEFAULT_REPLICATION_CQ_OFFSET_1 = new StorageSpec[]{
		new StorageSpec(CLOCK_QUADRANT_KEYS_OFFSET_1[0],1),
		new StorageSpec(CLOCK_QUADRANT_KEYS_OFFSET_1[1],1),
		new StorageSpec(CLOCK_QUADRANT_KEYS_OFFSET_1[2],1),
		new StorageSpec(CLOCK_QUADRANT_KEYS_OFFSET_1[3],1)};

	public static final StorageSpec[] DEFAULT_REPLICATION_CQ_OFFSET_1_PLUS_6XXX1 = new StorageSpec[]{
		new StorageSpec(CLOCK_QUADRANT_KEYS_OFFSET_1_PLUS_6XXX1[0],1),
		new StorageSpec(CLOCK_QUADRANT_KEYS_OFFSET_1_PLUS_6XXX1[1],1),
		new StorageSpec(CLOCK_QUADRANT_KEYS_OFFSET_1_PLUS_6XXX1[2],1),
		new StorageSpec(CLOCK_QUADRANT_KEYS_OFFSET_1_PLUS_6XXX1[4],1)};

	public static final IKey ARBITRARY_KEY;
	public static final String ARBITRARY_KEY_STRING = "deadbeef";
	public static String hostname;


	static {
		SHA1KeyFactory kf = new SHA1KeyFactory();
		ARBITRARY_KEY=kf.recreateKey(ARBITRARY_KEY_STRING);
		try {
			//hostname = InetAddress.getLocalHost().getHostName();
			hostname = NetworkUtil.getLocalIPv4Address().getHostName();
		} catch (UnknownHostException e) { ErrorHandling.hardError("couldn't get local host"); }
	}


	//	/**
	//	 * Route to each node key in 'keys' from each gateway in 'gateways'. 'keys' must contain only the node keys for
	//	 * nodes in teh network. The ring may not have formed yet and so this method will loop until the lookup key matches
	//	 * the node key for the root node for each key in 'keys', then it will return. Before the ring is formed the lookup key will not match
	//	 * the node key for the root.
	//	 *
	//	 * @param gateways the gateways from which to route
	//	 * @param keys the node keys for network nodes
	//	 * @throws P2PNodeException
	//	 */
	//	public static void pollNetwork_AllHostKeysFromAllGateways(IP2PRoutingAPI[] gateways, IKey[] keys) throws P2PNodeException {
	//		for(IP2PRoutingAPI gateway:gateways){
	//			for(IKey k:keys){
	//				IKey nodeKey=null;
	//				do{
	//				//System.out.println("lookup key = " + k);
	//				IP2PNode p2pnode=gateway.lookup(k);
	//				nodeKey=p2pnode.getKey();
	//				//System.out.println("node key = " + nodeKey);
	//				assertNotNull(nodeKey);
	//				}while(!k.equals(nodeKey));
	//			}
	//		}
	//	}

	/**
	 * This method will return when all nodes in the network have registered with the gateway and the network's routing
	 * mesh has been fully constructed. The number of expected nodes in the network is implicitly the number of
	 * elements in the 'keys' array. The network is created using the specified INetworkFactory implementation. All
	 * nodes have registered when a call to findAll() in the IGatewayLocator implementation returned by the factory
	 * method contains N nodes where N == keys.length. The routing mesh has been fully constructed when for
	 * each key value K represented in the keys array, the key K key routes to the node whose key is K.
	 *
	 * @param netFac
	 * @param keyStrings
	 * @throws Exception
	 * @throws P2PNodeException
	 */
	/*public static IChordNetwork makeStableNetwork(IChordNetworkFactory netFac, String[] keyStrings) throws Exception, P2PNodeException {
		IChordNetwork network = netFac.makeNetworkWithGateway(keyStrings);
		IGatewayLocator gl = network.getGatewayLocator();

		//wait for all expected nodes to register
		loopUntilGetNGateways(keyStrings.length, gl);

		IKey[] keys=keyStrings2Keys(keyStrings);
		//wait for the routing mesh to be fully established
		pollNetwork_AllHostKeysFromAllGateways(gl, keys);
		return network;
	}*/

	/**
	 * This method will return when all nodes in the network have registered with the gateway and the network's routing
	 * mesh has been fully constructed. It is assumed that the underlying overlay network implements the JChord
	 * protocol. The number of nodes in the network is explicitly stated with the parameter 'n'. The network is
	 * created using the specified INetworkFactory implementation. All nodes have registered when a call to findAll()
	 * in the IGatewayLocator implementation returned by the factory method contains 'n' nodes. The routing mesh has
	 * been fully constructed when a ring containing 'n' nodes has been formed and that ring can be traversed by
	 * following the predecessor and successor pointer. Note that this method is Chord specific.
	 *
	 * @param netFac
	 * @param n the number of nodes in the network
	 * @throws Exception
	 * @throws P2PNodeException
	 */
	/*public static IChordNetwork makeStableNetwork(IChordNetworkFactory netFac, int n) throws Exception, P2PNodeException {
		IChordNetwork network = netFac.makeNetworkWithGateway(n);
		IGatewayLocator gl = network.getGatewayLocator();
		//wait for all expected nodes to register
		loopUntilGetNGateways(n, gl);
		network.waitForStableNetwork();

//		List<INodeHandle> nodes = network.getNodes();
//		IKey[] keys=new IKey[n];
//		int index=0;
//		for(INodeHandle node:nodes)keys[index++]=node.getNodekey();
//		//wait for the routing mesh to be fully established
//		pollNetwork_AllHostKeysFromAllGateways(gl, keys);
		//traverseChordRing(network,n);

		return network;
	}*/



	/**
	 * Translates an array of strings representing key into a corresponding array of IKey implementations.
	 * @param keyStrings an array of strings representing keys. This method assumes that the the key values are represented in hexadecimal.
	 * @return an array of IKey implementations.
	 */
	public static IKey[] keyStrings2Keys(String[] keyStrings){
		HashBasedKeyFactory key_factory = new SHA1KeyFactory();

		IKey[] keys=new IKey[keyStrings.length];
		for(int i=0;i<keyStrings.length;i++) {
			keys[i]=key_factory.recreateKey(keyStrings[i]);
		}
		return keys;
	}

	public static List<String> makeKnownWithKeyArgs(String keyString, int servicePort, int knownPort, int gatewayLocPort){
		List<String> list = new ArrayList<String>();
		list.addAll(makeKeyArgs(keyString));
		list.addAll(makeKnownArgs(servicePort, knownPort, gatewayLocPort));
		return list;
	}

	public static List<String> makeKnownArgs(int servicePort, int knownPort){
		return makeKnownArgs(servicePort, knownPort, 0);
	}

	public static List<String> makeKnownArgs(int servicePort, int knownPort, int gatewayLocPort){
		List<String> list = new ArrayList<String>();
		list.add("-k"+hostname+":"+knownPort);
		list.addAll(makeServiceArgs(servicePort, gatewayLocPort));
		return list;
	}

	public static List<String> makeServiceArgs(int servicePort){
		return makeServiceArgs(servicePort, 0);
	}

	public static List<String> makeServiceArgs(int servicePort, int gatewayLocPort){
		List<String> list = new ArrayList<String>();
		list.add("-s"+hostname+":"+servicePort);
		if(gatewayLocPort>0) {
			list.addAll(makeGatewayLocArgs(gatewayLocPort));
		}
		return list;
	}

	public static List<String> makeServiceWithKeyArgs(String keyString, int servicePort, int gatewayLocPort){
		List<String> list = new ArrayList<String>();
		list.addAll(makeKeyArgs(keyString));
		list.addAll(makeServiceArgs(servicePort, gatewayLocPort));
		return list;
	}

	public static List<String> makeGatewayLocArgs(int gatewayLocPort) {
		List<String> list = new ArrayList<String>();
		list.add("-g"+hostname+":"+gatewayLocPort);
		return list;
	}

	public static List<String> makeKeyArgs(String keyString) {
		List<String> list = new ArrayList<String>();
		list.add("-l"+keyString);
		return list;
	}

	public static String[] argList2Array(List<String> list){
		return list.toArray(new String[0]);
	}

	public static void traverseChordRing(IChordNode startNode, int size) throws Exception {
		traverseChordRing(startNode,size,true);
		//anti-clockwise traversal
		traverseChordRing(startNode,size,false);
	}

	private static void traverseChordRing(IChordRemote startNode, int n, boolean clockwise) throws Exception {
		int count=0;
		do {
			try{
				count=0;
				IChordRemote next = clockwise?startNode.getSuccessor():startNode.getPredecessor();
				count++;
				while (next!=null && !next.getKey().equals(startNode.getKey())) {
					next = clockwise?next.getSuccessor():next.getPredecessor();
					count++;
				}
			}catch(Exception e){
				//do nothing
			}
		} while(count!=n);
	}

	public static Process runServer(Class<?> server_class, List<String> args) throws Exception {
		System.out.println("Running: "+server_class+"\n\t with args: "+args);
		Process p = Processes.runJavaProcess(server_class, args);
		return p;
	}


}
