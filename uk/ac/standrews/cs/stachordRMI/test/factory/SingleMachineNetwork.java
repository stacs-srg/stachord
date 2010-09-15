/*******************************************************************************
 * StAChord Library
 * Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 * http://asa.cs.st-andrews.ac.uk/
 * 
 * This file is part of stachordRMI.
 * 
 * stachordRMI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * stachordRMI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with stachordRMI.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.remote_management.server.HostDescriptor;
import uk.ac.standrews.cs.remote_management.server.UnknownPlatformException;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Network comprising Chord nodes all running on the local machine.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class SingleMachineNetwork extends MultipleMachineNetwork {

	private static final String LOCAL_HOST = "localhost";

	public SingleMachineNetwork(int number_of_nodes, KeyDistribution key_distribution) throws IOException, NotBoundException, InterruptedException {
		
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
		catch (TimeoutException e) {
			ErrorHandling.hardExceptionError(e, "unexpected timeout on local network creation");
		}
		catch (UnknownPlatformException e) {
			ErrorHandling.hardExceptionError(e, "unexpected unknown platform on local network creation");
		}
	}
}
