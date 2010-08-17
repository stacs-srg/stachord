package uk.ac.standrews.cs.stachordRMI.test.recovery;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

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

public class GangliaRecoveryTests {

	/**
	 * Runs a multiple machine test using public key authentication and dynamically installing libraries on remote machines.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws SSH2Exception
	 * @throws UnequalArrayLengthsException 
	 * @throws InterruptedException 
	 */
	@Test
	public void gangliaTestPublicKeyLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException, InterruptedException {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);
			
		// TODO Revert to 'lastStableBuild' when testing has been re-enabled.
		
		URL[] lib_urls = new URL[] {
				new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastBuild/artifact/bin/nds.jar"),
				new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastBuild/artifact/bin/stachordRMI.jar")
			};
			
		InetAddress[] addresses = getGangliaNodeAddresses();
		String[] java_versions =  getGangliaJavaVersions(addresses.length);
		File[] wget_paths =       getGangliaWgetPaths(addresses.length);
		File[] lib_install_dirs = getGangliaLibInstallDirs(addresses.length);
			
		SSH2ConnectionWrapper[] connections = NetworkUtil.createPublicKeyConnections(addresses, true);
		NodeDescriptor[] node_descriptors =   NetworkUtil.createNodeDescriptors(connections, java_versions, lib_urls, wget_paths, lib_install_dirs);
			
		TestLogic.ringRecoversFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM));

		System.out.println(">>>>> recovery test completed");
	}

	protected InetAddress[] getGangliaNodeAddresses() throws UnknownHostException {
		
		List<InetAddress> address_list = new ArrayList<InetAddress>();
		
		for (int index = 0; index <= 57; index++) {
			address_list.add(InetAddress.getByName("compute-0-" + index));
		}
		
		// Remove bad nodes.
		address_list.remove(InetAddress.getByName("compute-0-42"));
		address_list.remove(InetAddress.getByName("compute-0-46"));
		address_list.remove(InetAddress.getByName("compute-0-53"));

		return address_list.toArray(new InetAddress[] {});
	}

	protected File[] getGangliaLibInstallDirs(int number_of_nodes) {
		
		File[] lib_install_dirs = new File[number_of_nodes];
		
		for (int index = 0; index < lib_install_dirs.length; index++) {
			lib_install_dirs[index] = new File(Processes.DEFAULT_TEMP_PATH_LINUX);
		}
		return lib_install_dirs;
	}

	protected File[] getGangliaWgetPaths(int number_of_nodes) {
		
		File[] wget_paths = new File[number_of_nodes];
		
		for (int index = 0; index < wget_paths.length; index++) {
			wget_paths[index] = new File(Processes.DEFAULT_WGET_PATH_LINUX);
		}
		return wget_paths;
	}

	protected String[] getGangliaJavaVersions(int number_of_nodes) {
		
		String[] java_versions = new String[number_of_nodes];
		
		for (int index = 0; index < java_versions.length; index++) {
			java_versions[index] = "1.6.0_07";
		}
		return java_versions;
	}
}
