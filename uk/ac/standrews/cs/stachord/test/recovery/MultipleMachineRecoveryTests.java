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
package uk.ac.standrews.cs.stachord.test.recovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.nds.remote_management.UnknownPlatformException;
import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachord.test.factory.MultipleMachineNetwork;
import uk.ac.standrews.cs.stachord.test.factory.NetworkUtil;
import uk.ac.standrews.cs.stachord.test.factory.UnequalArrayLengthsException;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Various tests of small ring recovery, not intended to be run automatically.
 * They could be refactored to reduce code duplication, but it seems useful to keep them as self-contained examples.
 * 
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class MultipleMachineRecoveryTests {
	
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
		
		List<ClassPath> class_paths = new ArrayList<ClassPath>();
		class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/remote_management/lastStable/archive/bin/remote_management.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
		class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/remote_management/lastStable/archive/bin/remote_management.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
		class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/remote_management.jar:/Users/graham/stachordRMI.jar"));
		class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/remote_management.jar:/Users/graham/stachordRMI.jar"));

		NetworkUtil<IChordRemoteReference> network_util = new NetworkUtil<IChordRemoteReference>();
		List<SSH2ConnectionWrapper> connections =  network_util.createUsernamePasswordConnections(addresses, true);
		List<HostDescriptor> node_descriptors = network_util.createHostDescriptorsWithoutLibInstallation(connections, class_paths);
			
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
		
		List<ClassPath> class_paths = new ArrayList<ClassPath>();
		class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/remote_management/lastStable/archive/bin/remote_management.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
		class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/remote_management/lastStable/archive/bin/remote_management.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
		class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/remote_management.jar:/Users/graham/stachordRMI.jar"));
		class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/remote_management.jar:/Users/graham/stachordRMI.jar"));
			
		NetworkUtil<IChordRemoteReference> network_util = new NetworkUtil<IChordRemoteReference>();
		List<SSH2ConnectionWrapper> connections = network_util.createPublicKeyConnections(addresses, true);
		List<HostDescriptor> node_descriptors = network_util.createHostDescriptorsWithoutLibInstallation(connections, class_paths);
			
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
		
		URL[] lib_urls = new URL[] {
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"),
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/remote_management/lastStableBuild/artifact/bin/remote_management.jar"),
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar")
		};
			
		NetworkUtil<IChordRemoteReference> network_util = new NetworkUtil<IChordRemoteReference>();
		List<SSH2ConnectionWrapper> connections = network_util.createUsernamePasswordConnections(addresses, true);
		List<HostDescriptor> node_descriptors = network_util.createHostDescriptorsWithLibInstallation(connections, lib_urls);
			
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
		
		URL[] lib_urls = new URL[] {
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"),
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/remote_management/lastStableBuild/artifact/bin/remote_management.jar"),
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar")
		};

		NetworkUtil<IChordRemoteReference> network_util = new NetworkUtil<IChordRemoteReference>();
		List<SSH2ConnectionWrapper> connections = network_util.createPublicKeyConnections(addresses, true);
		List<HostDescriptor> node_descriptors = network_util.createHostDescriptorsWithLibInstallation(connections, lib_urls);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM), 500);

		System.out.println(">>>>> recovery test completed");
	}
}