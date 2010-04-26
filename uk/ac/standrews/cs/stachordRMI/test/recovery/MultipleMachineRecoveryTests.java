package uk.ac.standrews.cs.stachordRMI.test.recovery;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.rmi.NotBoundException;

import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.stachordRMI.test.factory.MultipleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.NodeDescriptor;
import uk.ac.standrews.cs.stachordRMI.test.util.TestLogic;

import com.mindbright.ssh2.SSH2Exception;

public class MultipleMachineRecoveryTests {

	public static void main(String[] args) throws IOException, NotBoundException, SSH2Exception {
		
		InetAddress address = InetAddress.getByName("beast.cs.st-andrews.ac.uk");
		String java_version = "1.5.0_14";
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
//		System.out.print("enter remote username: ");
//		String username = reader.readLine();
//		
//		System.out.print("enter remote password: ");
//		String password = reader.readLine();
//		
//		SSH2ConnectionWrapper connection_wrapper = new SSH2ConnectionWrapper(address, username, password);

		
		
		String user = "graham";
		String privateKeyFilePath = "/Users/graham/.ssh/id_rsa";

		System.out.print("enter ssh passphrase :");

		String passPhrase = reader.readLine();
		
		SSH2ConnectionWrapper connection_wrapper = new SSH2ConnectionWrapper(address, user, privateKeyFilePath, passPhrase);

		
		
		ClassPath class_path = new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar");
		
		NodeDescriptor[] node_descriptors = new NodeDescriptor[] {
				new NodeDescriptor(connection_wrapper, java_version, class_path),
				new NodeDescriptor(connection_wrapper, java_version, class_path),
				new NodeDescriptor(connection_wrapper, java_version, class_path),
				new NodeDescriptor(connection_wrapper, java_version, class_path),
				new NodeDescriptor(connection_wrapper, java_version, class_path)
		};
			
		TestLogic.ringRecovers(new MultipleMachineNetwork(node_descriptors, MultipleMachineNetwork.RANDOM));

		System.out.println(">>>>> recovery test completed");
	}
}
