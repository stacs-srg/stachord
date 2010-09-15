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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.remote_management.server.ClassPath;
import uk.ac.standrews.cs.remote_management.server.HostDescriptor;
import uk.ac.standrews.cs.remote_management.server.ProcessInvocation;
import uk.ac.standrews.cs.remote_management.server.UnknownPlatformException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachordRMI.test.factory.MultipleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.NetworkUtil;
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
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws UnknownPlatformException 
	 */
	@Test
	public void multiMachineTestPasswordNoLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException, InterruptedException, TimeoutException, UnknownPlatformException {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);

		List<InetAddress> addresses = new ArrayList<InetAddress>();
		addresses.add(InetAddress.getByName("beast.cs.st-andrews.ac.uk"));
		addresses.add(InetAddress.getByName("beast.cs.st-andrews.ac.uk"));
		addresses.add(InetAddress.getByName("mini.cs.st-andrews.ac.uk"));
		addresses.add(InetAddress.getByName("mini.cs.st-andrews.ac.uk"));
		
		List<String> java_versions = new ArrayList<String>();
		java_versions.add("1.6.0_21");
		java_versions.add("1.6.0_21");
		java_versions.add("1.6.0_20");
		java_versions.add("1.6.0_20");
		
		List<ClassPath> class_paths = new ArrayList<ClassPath>();
		class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
		class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
		class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/stachordRMI.jar"));
		class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/stachordRMI.jar"));

		NetworkUtil<IChordRemoteReference> network_util = new NetworkUtil<IChordRemoteReference>();
		List<SSH2ConnectionWrapper> connections =  network_util.createUsernamePasswordConnections(addresses, true);
		List<HostDescriptor> node_descriptors = network_util.createNodeDescriptors(connections, java_versions, class_paths);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM), 500);

		System.out.println(">>>>> recovery test completed");
	}
	
	/**
	 * Runs a multiple machine test using public key authentication and assuming that libraries are pre-installed on remote machines.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws SSH2Exception
	 * @throws UnequalArrayLengthsException 
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws UnknownPlatformException 
	 */
	@Test
	public void multiMachineTestPublicKeyNoLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException, InterruptedException, TimeoutException, UnknownPlatformException {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);

		List<InetAddress> addresses = new ArrayList<InetAddress>();
		addresses.add(InetAddress.getByName("beast.cs.st-andrews.ac.uk"));
		addresses.add(InetAddress.getByName("beast.cs.st-andrews.ac.uk"));
		addresses.add(InetAddress.getByName("mini.cs.st-andrews.ac.uk"));
		addresses.add(InetAddress.getByName("mini.cs.st-andrews.ac.uk"));
			
		List<String> java_versions = new ArrayList<String>();
		java_versions.add("1.6.0_03");
		java_versions.add("1.6.0_03");
		java_versions.add("1.6.0_20");
		java_versions.add("1.6.0_20");
		
		List<ClassPath> class_paths = new ArrayList<ClassPath>();
		class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
		class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
		class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/stachordRMI.jar"));
		class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/stachordRMI.jar"));
			
		NetworkUtil<IChordRemoteReference> network_util = new NetworkUtil<IChordRemoteReference>();
		List<SSH2ConnectionWrapper> connections = network_util.createPublicKeyConnections(addresses, true);
		List<HostDescriptor> node_descriptors = network_util.createNodeDescriptors(connections, java_versions, class_paths);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM), 500);

		System.out.println(">>>>> recovery test completed");
	}
	
	/**
	 * Runs a multiple machine test using password authentication and dynamically installing libraries on remote machines.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws SSH2Exception
	 * @throws UnequalArrayLengthsException 
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws UnknownPlatformException 
	 */
	@Test
	public void multiMachineTestPasswordLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException, InterruptedException, TimeoutException, UnknownPlatformException {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);

		List<InetAddress> addresses = new ArrayList<InetAddress>();
		addresses.add(InetAddress.getByName("beast.cs.st-andrews.ac.uk"));
		addresses.add(InetAddress.getByName("beast.cs.st-andrews.ac.uk"));
		addresses.add(InetAddress.getByName("mini.cs.st-andrews.ac.uk"));
		addresses.add(InetAddress.getByName("mini.cs.st-andrews.ac.uk"));
		
		List<String> java_versions = new ArrayList<String>();
		java_versions.add("1.6.0_21");
		java_versions.add("1.6.0_21");
		java_versions.add("1.6.0_20");
		java_versions.add("1.6.0_20");
		
		List<URL> lib_urls = new ArrayList<URL>();
		lib_urls.add(new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"));
		lib_urls.add(new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar"));
			
		List<File> wget_paths = new ArrayList<File>();
		wget_paths.add(new File(ProcessInvocation.DEFAULT_WGET_PATH_LINUX));
		wget_paths.add(new File(ProcessInvocation.DEFAULT_WGET_PATH_LINUX));
		wget_paths.add(new File(ProcessInvocation.DEFAULT_WGET_PATH_MAC));
		wget_paths.add(new File(ProcessInvocation.DEFAULT_WGET_PATH_MAC));
			
		List<File> lib_install_dirs = new ArrayList<File>();
		lib_install_dirs.add(new File(ProcessInvocation.DEFAULT_TEMP_PATH_LINUX));
		lib_install_dirs.add(new File(ProcessInvocation.DEFAULT_TEMP_PATH_LINUX));
		lib_install_dirs.add(new File(ProcessInvocation.DEFAULT_TEMP_PATH_MAC));
		lib_install_dirs.add(new File(ProcessInvocation.DEFAULT_TEMP_PATH_MAC));
			
		NetworkUtil<IChordRemoteReference> network_util = new NetworkUtil<IChordRemoteReference>();
		List<SSH2ConnectionWrapper> connections = network_util.createUsernamePasswordConnections(addresses, true);
		List<HostDescriptor> node_descriptors = network_util.createNodeDescriptors(connections, java_versions, lib_urls, wget_paths, lib_install_dirs);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM), 60000);

		System.out.println(">>>>> recovery test completed");
	}

	/**
	 * Runs a multiple machine test using public key authentication and dynamically installing libraries on remote machines.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws SSH2Exception
	 * @throws UnequalArrayLengthsException 
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws UnknownPlatformException 
	 */
	@Test
	public void multiMachineTestPublicKeyLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException, InterruptedException, TimeoutException, UnknownPlatformException {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);

		List<InetAddress> addresses = new ArrayList<InetAddress>();
		addresses.add(InetAddress.getByName("compute-0-33"));
		addresses.add(InetAddress.getByName("compute-0-34"));
		addresses.add(InetAddress.getByName("compute-0-35"));

		List<String> java_versions = new ArrayList<String>();
		java_versions.add("1.6.0_07");
		java_versions.add("1.6.0_07");
		java_versions.add("1.6.0_07");
		
		List<URL> lib_urls = new ArrayList<URL>();
		lib_urls.add(new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"));
		lib_urls.add(new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar"));
			
		List<File> wget_paths = new ArrayList<File>();
		wget_paths.add(new File(ProcessInvocation.DEFAULT_WGET_PATH_LINUX));
		wget_paths.add(new File(ProcessInvocation.DEFAULT_WGET_PATH_LINUX));
		wget_paths.add(new File(ProcessInvocation.DEFAULT_WGET_PATH_LINUX));
			
		List<File> lib_install_dirs = new ArrayList<File>();
		lib_install_dirs.add(new File(ProcessInvocation.DEFAULT_TEMP_PATH_LINUX));
		lib_install_dirs.add(new File(ProcessInvocation.DEFAULT_TEMP_PATH_LINUX));
		lib_install_dirs.add(new File(ProcessInvocation.DEFAULT_TEMP_PATH_LINUX));

		NetworkUtil<IChordRemoteReference> network_util = new NetworkUtil<IChordRemoteReference>();
		List<SSH2ConnectionWrapper> connections = network_util.createPublicKeyConnections(addresses, true);
		List<HostDescriptor> node_descriptors = network_util.createNodeDescriptors(connections, java_versions, lib_urls, wget_paths, lib_install_dirs);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM), 500);

		System.out.println(">>>>> recovery test completed");
	}
}