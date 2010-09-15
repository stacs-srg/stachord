package uk.ac.standrews.cs.stachordRMI.test.recovery;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.remote_management.server.HostDescriptor;
import uk.ac.standrews.cs.remote_management.server.ProcessInvocation;
import uk.ac.standrews.cs.remote_management.server.UnknownPlatformException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachordRMI.test.factory.MultipleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.NetworkUtil;
import uk.ac.standrews.cs.stachordRMI.test.factory.UnequalArrayLengthsException;

import com.mindbright.ssh2.SSH2Exception;

public class GangliaRecoveryTests {

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
	public void gangliaTestPublicKeyLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException, InterruptedException, TimeoutException, UnknownPlatformException {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);
		
		List<URL> lib_urls = new ArrayList<URL>();
		lib_urls.add(new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"));
		lib_urls.add(new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar"));
			
		List<InetAddress> addresses = getGangliaNodeAddresses();
		List<String> java_versions =  getGangliaJavaVersions(addresses.size());
		List<File> wget_paths =       getGangliaWgetPaths(addresses.size());
		List<File> lib_install_dirs = getGangliaLibInstallDirs(addresses.size());

		NetworkUtil<IChordRemoteReference> network_util = new NetworkUtil<IChordRemoteReference>();
		
		List<SSH2ConnectionWrapper> connections = network_util.createPublicKeyConnections(addresses, true);
		List<HostDescriptor> node_descriptors = network_util.createNodeDescriptors(connections, java_versions, lib_urls, wget_paths, lib_install_dirs);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM), 500);

		System.out.println(">>>>> recovery test completed");
	}

	protected List<InetAddress> getGangliaNodeAddresses() throws UnknownHostException {
		
		List<InetAddress> address_list = new ArrayList<InetAddress>();
		
		for (int index = 0; index <= 57; index++) {
			address_list.add(InetAddress.getByName("compute-0-" + index));
		}
		
		// Remove bad nodes.
		address_list.remove(InetAddress.getByName("compute-0-42"));
		address_list.remove(InetAddress.getByName("compute-0-46"));
		address_list.remove(InetAddress.getByName("compute-0-53"));

		return address_list;
	}

	protected List<File> getGangliaLibInstallDirs(int number_of_nodes) {
		
		List<File> lib_install_dirs = new ArrayList<File>();
		
		for (int index = 0; index < number_of_nodes; index++) {
			lib_install_dirs.add(new File(ProcessInvocation.DEFAULT_TEMP_PATH_LINUX));
		}
		return lib_install_dirs;
	}

	protected List<File> getGangliaWgetPaths(int number_of_nodes) {
		
		List<File> wget_paths = new ArrayList<File>();
		
		for (int index = 0; index < number_of_nodes; index++) {
			wget_paths.add(new File(ProcessInvocation.DEFAULT_WGET_PATH_LINUX));
		}
		return wget_paths;
	}

	protected List<String> getGangliaJavaVersions(int number_of_nodes) {
		
		List<String> java_versions = new ArrayList<String>();
		
		for (int index = 0; index < number_of_nodes; index++) {
			java_versions.add("1.6.0_07");
		}
		return java_versions;
	}
}
