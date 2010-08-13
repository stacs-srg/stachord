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
package uk.ac.standrews.cs.stachordRMI.test.recovery;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.NotBoundException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.MaskedStringInput;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.stachordRMI.test.factory.MultipleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachordRMI.test.factory.NodeDescriptor;
import uk.ac.standrews.cs.stachordRMI.test.util.TestLogic;

import com.mindbright.ssh2.SSH2Exception;

public class MultipleMachineRecoveryTests {

	private static Map<InetAddress, SSH2ConnectionWrapper> connection_cache = new HashMap<InetAddress, SSH2ConnectionWrapper>();
	
	@Test
	public void multiMachineTest() throws IOException, NotBoundException, SSH2Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);

		InetAddress[] addresses = new InetAddress[] {
			InetAddress.getByName("beast.cs.st-andrews.ac.uk"),
			InetAddress.getByName("beast.cs.st-andrews.ac.uk"),
			InetAddress.getByName("mini.cs.st-andrews.ac.uk"),
			InetAddress.getByName("mini.cs.st-andrews.ac.uk")
		};
		
		String[] java_versions = new String[] {
			"1.6.0_03",
			"1.6.0_03",
			"1.6.0_20",
			"1.6.0_20"
		};
		
		ClassPath[] class_paths = new ClassPath[] {
			new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"),
			new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"),
			new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"),
			new ClassPath("/user/graham/nds.jar:/user/graham/stachordRMI.jar"),
			new ClassPath("/user/graham/nds.jar:/user/graham/stachordRMI.jar")
		};
		
		URL[] lib_urls = new URL[] {
				new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"),
				new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar")
			};
			
		File[] wget_paths = new File[] {
				new File(Processes.DEFAULT_WGET_PATH_LINUX),
				new File(Processes.DEFAULT_WGET_PATH_LINUX),
				new File(Processes.DEFAULT_WGET_PATH_MAC),
				new File(Processes.DEFAULT_WGET_PATH_MAC)
			};
			
		File[] lib_install_dirs = new File[] {
				new File(Processes.DEFAULT_TEMP_PATH_LINUX),
				new File(Processes.DEFAULT_TEMP_PATH_LINUX),
				new File(Processes.DEFAULT_TEMP_PATH_MAC),
				new File(Processes.DEFAULT_TEMP_PATH_MAC)
			};
			
//		SSH2ConnectionWrapper[] connections = createPublicKeyConnections(addresses);

		// Create 
		 SSH2ConnectionWrapper[] connections = createUsernamePasswordConnections(addresses);
	
//		NodeDescriptor[] node_descriptors1 = createNodeDescriptors(connections, java_versions, class_paths);
		NodeDescriptor[] node_descriptors = createNodeDescriptors(connections, java_versions, lib_urls, wget_paths, lib_install_dirs);
			
		TestLogic.ringRecovers(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM));

		System.out.println(">>>>> recovery test completed");
	}

	private static SSH2ConnectionWrapper[] createUsernamePasswordConnections(InetAddress[] addresses) throws IOException {
		
		SSH2ConnectionWrapper[] connections = new SSH2ConnectionWrapper[addresses.length];
		for (int i = 0; i < addresses.length; i++) {
			connections[i] = createUsernamePasswordConnection(addresses[i]);
		}
		return connections;
	}

	private static SSH2ConnectionWrapper[] createPublicKeyConnections(InetAddress[] addresses) throws IOException {

		SSH2ConnectionWrapper[] connections = new SSH2ConnectionWrapper[addresses.length];
		for (int i = 0; i < addresses.length; i++) {
			connections[i] = createPublicKeyConnection(addresses[i]);
		}
		return connections;
	}

	protected static NodeDescriptor[] createNodeDescriptors(SSH2ConnectionWrapper[] connections, String[] java_versions, ClassPath[] class_paths) {
		
		NodeDescriptor[] node_descriptors = new NodeDescriptor[connections.length];
		for (int i = 0; i < connections.length; i++) {
			node_descriptors[i] = new NodeDescriptor(connections[i], java_versions[i], class_paths[i]);
		}
		return node_descriptors;
	}

	protected static NodeDescriptor[] createNodeDescriptors(SSH2ConnectionWrapper[] connections, String[] java_versions, URL[] lib_urls, File[] wget_paths, File[] lib_install_dirs) {
		
		NodeDescriptor[] node_descriptors = new NodeDescriptor[connections.length];
		for (int i = 0; i < connections.length; i++) {
			node_descriptors[i] = new NodeDescriptor(connections[i], java_versions[i], lib_urls, wget_paths[i], lib_install_dirs[i]);
		}
		return node_descriptors;
	}

	protected static SSH2ConnectionWrapper createUsernamePasswordConnection(InetAddress address) throws IOException {
		
		SSH2ConnectionWrapper wrapper = connection_cache.get(address);
		
		if (wrapper != null) return wrapper;
		else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.print("enter remote username for " + address.getCanonicalHostName() + ": ");
			String username = reader.readLine();
				
			String password = MaskedStringInput.getMaskedString("enter remote password for " + address.getCanonicalHostName() + ":");
		
			wrapper = new SSH2ConnectionWrapper(address, username, password);
			connection_cache.put(address, wrapper);
			return wrapper;
		}
	}
	
	protected static SSH2ConnectionWrapper createPublicKeyConnection(InetAddress address) throws IOException {
		
		SSH2ConnectionWrapper wrapper = connection_cache.get(address);
		
		if (wrapper != null) return wrapper;
		else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.print("enter remote username for " + address.getCanonicalHostName() + ": ");
			String username = reader.readLine();
			
			String private_key_file_path = new File(new File(System.getProperty("user.home"), ".ssh"), "id_rsa").getAbsolutePath();
			String pass_phrase = MaskedStringInput.getMaskedString("enter SSH passphrase for " + address.getCanonicalHostName() + ":");
		
			wrapper = new SSH2ConnectionWrapper(address, username, private_key_file_path, pass_phrase);
			connection_cache.put(address, wrapper);
			return wrapper;
		}
	}
}
