package uk.ac.standrews.cs.stachord.test.recovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.nds.remote_management.UnknownPlatformException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachord.test.factory.MultipleMachineNetwork;
import uk.ac.standrews.cs.stachord.test.factory.NetworkUtil;
import uk.ac.standrews.cs.stachord.test.factory.UnequalArrayLengthsException;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Various tests of ring recovery on the Ganglia cluster, not intended to be run automatically.
 * 
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
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
		
		URL[] lib_urls = new URL[] {
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"),
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar")
		};
			
		List<InetAddress> addresses = getGangliaNodeAddresses();
		
		List<SSH2ConnectionWrapper> connections = NetworkUtil.createPublicKeyConnections(addresses, true);
		List<HostDescriptor> node_descriptors = NetworkUtil.createHostDescriptors(connections, lib_urls);
			
		RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleMachineNetwork(node_descriptors, KeyDistribution.RANDOM), 500);

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
}
