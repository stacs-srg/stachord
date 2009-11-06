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

import java.util.List;

public interface IP2PNetwork<T> {

	List<IChordNetworkNodeHandle<T>> getNodes();
	void addNode(String k) throws Exception;
	void addNode() throws Exception;
	/**
	 * Kill all nodes in the network and the gateway locator (if the network has one).
	 * @throws Exception
	 */
	void killNetwork() throws Exception;

	/**
	 * Kill all the specified node in the network.
	 * @throws Exception
	 */
	void killNode(IChordNetworkNodeHandle<T> node) throws Exception;

	/**
	 * Return when the network contains only those nodes that it is expected to contain and the routing mesh implements
	 * the correct key mappings.
	 */
	void waitForStableNetwork() throws Exception;

	boolean isNetworkStable() throws Exception;
}
