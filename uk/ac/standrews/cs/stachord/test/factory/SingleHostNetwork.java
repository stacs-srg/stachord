/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of stachord, an independent implementation of         *
 * the Chord protocol (http://pdos.csail.mit.edu/chord/).                  *
 *                                                                         *
 * stachord is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * stachord is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with stachord.  If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                         *
 ***************************************************************************/

package uk.ac.standrews.cs.stachord.test.factory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.UnknownPlatformException;
import uk.ac.standrews.cs.nds.util.ErrorHandling;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Network comprising Chord nodes all running on the local machine.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class SingleHostNetwork extends MultipleHostNetwork {

	private static final String LOCAL_HOST = "localhost";

	/**
	 * Creates a new network.
	 * 
	 * @param number_of_nodes the number of nodes to be created
	 * @param key_distribution the required key distribution
	 * 
	 * @throws IOException if the process for a node cannot be created
	 * @throws TimeoutException if one or more nodes cannot be instantiated within the timeout period
	 * @throws UnknownPlatformException if the operating system of the local host cannot be established
	 * @throws InterruptedException if there is an error during concurrent instantiation of the nodes
	 */
	public SingleHostNetwork(int number_of_nodes, KeyDistribution key_distribution) throws IOException, TimeoutException, UnknownPlatformException, InterruptedException {
		
		try {
			List<HostDescriptor> node_descriptors = new ArrayList<HostDescriptor>();
			
			for (int i = 0; i < number_of_nodes; i++) {
				node_descriptors.add(new HostDescriptor(LOCAL_HOST));
			}

			init(node_descriptors, key_distribution);
		}
		catch (SSH2Exception e) {
			ErrorHandling.hardExceptionError(e, "unexpected SSH error on local network creation");
		}
	}
}
