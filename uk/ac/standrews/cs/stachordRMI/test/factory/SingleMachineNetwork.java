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
import java.util.List;

import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.stachordRMI.servers.AbstractServer;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Network comprising Chord nodes all running on the local machine.
 * 
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class SingleMachineNetwork extends MultipleMachineNetwork {

	static final String LOCAL_HOST = "localhost";

	public SingleMachineNetwork(int number_of_nodes, KeyDistribution key_distribution) throws IOException, NotBoundException {
		
		try {
			// The node descriptors will be null but that's OK because their values are ignored by the overriding methods below.
			init(new NodeDescriptor[number_of_nodes], key_distribution);
		}
		catch (SSH2Exception e) {
			ErrorHandling.hardExceptionError(e, "unexpected SSH error on local network creation");
		}
	}

	protected Process runProcess(NodeDescriptor node_descriptor, Class<? extends AbstractServer> node_class, List<String> args) throws IOException, SSH2Exception {
		
		// Ignore node_descriptor for local process.
		return Processes.runJavaProcess(node_class, args);
	}
	
	protected String getHost(NodeDescriptor node_descriptor) {
		
		// Ignore node_descriptor for local process.
		return LOCAL_HOST;
	}
}
