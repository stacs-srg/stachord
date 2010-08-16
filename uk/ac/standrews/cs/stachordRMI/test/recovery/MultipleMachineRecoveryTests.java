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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.NotBoundException;

import org.junit.Test;

import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.stachordRMI.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachordRMI.test.factory.MultipleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.NetworkUtil;
import uk.ac.standrews.cs.stachordRMI.test.factory.NodeDescriptor;
import uk.ac.standrews.cs.stachordRMI.test.factory.UnequalArrayLengthsException;

import com.mindbright.ssh2.SSH2Exception;

public class MultipleMachineRecoveryTests {
	
	// These tests could be refactored to reduce code duplication, but it seems useful to keep them as self-contained examples.

	/**
	 * Runs a multiple machine test using password authentication and assuming that libraries are pre-installed on remote machines.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws SSH2Exception
	 * @throws UnequalArrayLengthsException 
	 */
	//@Test
	public void multiMachineTestPasswordNoLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException {
		
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
				new ClassPath("/Users/graham/nds.jar:/Users/graham/stachordRMI.jar"),
				new ClassPath("/Users/graham/nds.jar:/Users/graham/stachordRMI.jar"),
			};

		SSH2ConnectionWrapper[] connections = NetworkUtil.createUsernamePasswordConnections(addresses);
		NodeDescriptor[] node_descriptors =   NetworkUtil.createNodeDescriptors(connections, java_versions, class_paths);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM));

		System.out.println(">>>>> recovery test completed");
	}
	
	/**
	 * Runs a multiple machine test using public key authentication and assuming that libraries are pre-installed on remote machines.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws SSH2Exception
	 * @throws UnequalArrayLengthsException 
	 */
	//@Test
	public void multiMachineTestPublicKeyNoLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException {
		
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
				new ClassPath("/Users/graham/nds.jar:/Users/graham/stachordRMI.jar"),
				new ClassPath("/Users/graham/nds.jar:/Users/graham/stachordRMI.jar"),
			};		
			
		SSH2ConnectionWrapper[] connections = NetworkUtil.createPublicKeyConnections(addresses);
		NodeDescriptor[] node_descriptors =   NetworkUtil.createNodeDescriptors(connections, java_versions, class_paths);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM));

		System.out.println(">>>>> recovery test completed");
	}
	
	/**
	 * Runs a multiple machine test using password authentication and dynamically installing libraries on remote machines.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws SSH2Exception
	 * @throws UnequalArrayLengthsException 
	 */
	//@Test
	public void multiMachineTestPasswordLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException {
		
		Diagnostic.setLevel(DiagnosticLevel.FULL);

		InetAddress[] addresses = new InetAddress[] {
			InetAddress.getByName("beast.cs.st-andrews.ac.uk"),
			InetAddress.getByName("beast.cs.st-andrews.ac.uk"),
			InetAddress.getByName("blub.cs.st-andrews.ac.uk"),
			InetAddress.getByName("blub.cs.st-andrews.ac.uk")
		};
		
		String[] java_versions = new String[] {
			"1.6.0_03",
			"1.6.0_03",
			"1.6.0_07",
			"1.6.0_07"
		};
		
		URL[] lib_urls = new URL[] {
				new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"),
				new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar")
			};
			
		File[] wget_paths = new File[] {
				new File(Processes.DEFAULT_WGET_PATH_LINUX),
				new File(Processes.DEFAULT_WGET_PATH_LINUX),
				new File(Processes.DEFAULT_WGET_PATH_LINUX),
				new File(Processes.DEFAULT_WGET_PATH_LINUX)
			};
			
		File[] lib_install_dirs = new File[] {
				new File(Processes.DEFAULT_TEMP_PATH_LINUX),
				new File(Processes.DEFAULT_TEMP_PATH_LINUX),
				new File(Processes.DEFAULT_TEMP_PATH_LINUX),
				new File(Processes.DEFAULT_TEMP_PATH_LINUX)
			};
			
		SSH2ConnectionWrapper[] connections = NetworkUtil.createUsernamePasswordConnections(addresses);
		NodeDescriptor[] node_descriptors =   NetworkUtil.createNodeDescriptors(connections, java_versions, lib_urls, wget_paths, lib_install_dirs);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM));

		System.out.println(">>>>> recovery test completed");
	}

	/**
	 * Runs a multiple machine test using public key authentication and dynamically installing libraries on remote machines.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws SSH2Exception
	 * @throws UnequalArrayLengthsException 
	 */
	@Test
	public void multiMachineTestPublicKeyLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);

		InetAddress[] addresses = new InetAddress[] {
				InetAddress.getByName("compute-0-33"),
				InetAddress.getByName("compute-0-34"),
				InetAddress.getByName("compute-0-35")
			};
			
			String[] java_versions = new String[] {
					"1.6.0_07",
					"1.6.0_07",
					"1.6.0_07"
			};
		
		// TODO Revert to 'lastStableBuild' when testing has been re-enabled.
		
		URL[] lib_urls = new URL[] {
				new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastBuild/artifact/bin/nds.jar"),
				new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastBuild/artifact/bin/stachordRMI.jar")
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
			
		SSH2ConnectionWrapper[] connections = NetworkUtil.createPublicKeyConnections(addresses);
		NodeDescriptor[] node_descriptors =   NetworkUtil.createNodeDescriptors(connections, java_versions, lib_urls, wget_paths, lib_install_dirs);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM));

		System.out.println(">>>>> recovery test completed");
	}
}
