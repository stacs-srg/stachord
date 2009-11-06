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
package uk.ac.standrews.cs.stachordRMI.testharness.interfaces;

public interface IChordNetworkFactory<NodeType> {

	/**
	 * Create a network with a specified number of nodes and a gateway locator. The node keys are determined by policy
	 * in the node implementation.
	 *
	 * @param numNodes the number of nodes in the network
	 * @throws Exception
	 */
	IChordNetwork<NodeType> makeNetworkWithGateway(int numNodes) throws Exception;

	/**
	 * Create a network consisting of nodes with explicitly specified keys and a gateway locator.
	 *
	 * @param keyStrings  the keys for the network nodes
	 * @throws Exception
	 */
	IChordNetwork<NodeType> makeNetworkWithGateway(String[] keyStrings) throws Exception;
}
