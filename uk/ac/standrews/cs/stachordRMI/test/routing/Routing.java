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
package uk.ac.standrews.cs.stachordRMI.test.routing;

import static org.junit.Assert.assertTrue;

import java.util.List;

import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.testharness.interfaces.INetworkFactory;
import uk.ac.standrews.cs.stachord.testharness.interfaces.IChordNetwork;
import uk.ac.standrews.cs.stachordRMI.test.stuff.Util;

public class Routing {

	public static void ringRouting(INetworkFactory<?> fac) throws Exception {


		int MAX_NUMBER_OF_NODES = 10;
		int NODE_INCREMENT = 2;

		for (int number_of_nodes = 0; number_of_nodes <= MAX_NUMBER_OF_NODES; number_of_nodes += NODE_INCREMENT){
			/*
			 * Reset the port number sequence for new nodes.
			 */
			fac.resetPortNumber();

			IChordNetwork<?> net = fac.makeNetwork(number_of_nodes);
			net.waitForStableNetwork();
			assertTrue(net.getNodes().size()==number_of_nodes);

			List<IChordRemote> nodeList= Util.networkNodeHandles2IChordRemote(net);
			assertTrue(Util.isRoutingCorrectForSample(nodeList,1));

			/*
			 * Reset the factory so a new network can be created. It is not enough to simply kill all of the network
			 * nodes (i.e. network.killNetwork())
			 */
			fac.resetNetworkFactory();
		}
	}

	public static void ringRoutingRandomised(INetworkFactory<?> fac) throws Exception {

		int MAX_NUMBER_OF_NODES = 10;
		int NODE_INCREMENT = 2;

		for (int number_of_nodes = 0; number_of_nodes <= MAX_NUMBER_OF_NODES; number_of_nodes += NODE_INCREMENT) {
			/*
			 * Reset the port number sequence for new nodes.
			 */
			fac.resetPortNumber();

			IChordNetwork<?> net = fac.makeNetwork(number_of_nodes);
			net.waitForStableNetwork();
			assertTrue(net.getNodes().size()==number_of_nodes);

			List<IChordRemote> nodeList= Util.networkNodeHandles2IChordRemote(net);
			assertTrue(Util.isRoutingCorrectForSample(nodeList,NODE_INCREMENT));

			/*
			 * Reset the factory so a new network can be created. It is not enough to simply kill all of the network
			 * nodes (i.e. network.killNetwork())
			 */
			fac.resetNetworkFactory();
		}
	}
}
