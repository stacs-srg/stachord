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
package uk.ac.standrews.cs.stachordRMI.testharness.logic;

import static org.junit.Assert.assertTrue;

import java.util.List;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetwork;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetworkNodeHandle;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.INetworkFactory;

public class Recovery {

	private static final int MIN_NETWORK_SIZE = 5;
	private static final int MAX_NETWORK_SIZE = 20;
	private static final int NETWORK_SIZE_INCREMENT = 5;


	public static void twoNodeNetwork_killNode(INetworkFactory<?> fac) throws Exception{
		IChordNetwork<?> net = fac.makeNetwork(2);
		net.waitForStableNetwork();
		assertTrue(net.getNodes().size()==2);

		//network correct before node is killed
		List<IChordRemote> nodeList= Util.networkNodeHandles2IChordRemote(net);
		while(!Util.areFingerTablesConsistent(nodeList)){}
		while(!Util.areSuccessorListsConsistent(nodeList)){}
		IChordNetworkNodeHandle<?> node = net.getNodes().get(0);
		node.killNode();
		//network correct after node is killed
		net.waitForStableNetwork();
		assertTrue(net.getNodes().size()==1);
		nodeList= Util.networkNodeHandles2IChordRemote(net);
		while(!Util.areFingerTablesConsistent(nodeList)){}
		while(!Util.areSuccessorListsConsistent(nodeList)){}
	}

	public static void multiNodeNetworks_killNode(INetworkFactory<?> fac) throws Exception{
		for(int i=MIN_NETWORK_SIZE; i<=MAX_NETWORK_SIZE; i+=NETWORK_SIZE_INCREMENT){
			/*
			 * Reset the port number sequence for new nodes.
			 */
			fac.resetPortNumber();

			IChordNetwork<?> net = fac.makeNetwork(i);
			//network correct before node is killed
			List<IChordRemote> nodeList= Util.networkNodeHandles2IChordRemote(net);
			while(!Util.areFingerTablesConsistent(nodeList)){}
			while(!Util.areSuccessorListsConsistent(nodeList)){}
			IChordNetworkNodeHandle<?> node = net.getNodes().get(0);
			node.killNode();
			//network correct after node is killed
			net.waitForStableNetwork();
			assertTrue(net.getNodes().size()==i-1);
			nodeList= Util.networkNodeHandles2IChordRemote(net);
			while(!Util.areFingerTablesConsistent(nodeList)){}
			while(!Util.areSuccessorListsConsistent(nodeList)){}

			/*
			 * Reset the factory so a new network can be created. It is not enough to simply kill all of the network
			 * nodes (i.e. network.killNetwork())
			 */
			fac.resetNetworkFactory();
		}
	}

}
