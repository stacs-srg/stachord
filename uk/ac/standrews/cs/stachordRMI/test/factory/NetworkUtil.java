package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.mindbright.ssh2.SSH2Exception;

import uk.ac.standrews.cs.nds.util.MaskedStringInput;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.remote_management.server.HostDescriptor;
import uk.ac.standrews.cs.remote_management.util.ClassPath;

public class NetworkUtil<ApplicationReference> {

	public List<SSH2ConnectionWrapper> createUsernamePasswordConnections(List<InetAddress> addresses, boolean same_credentials_for_all) throws IOException {
		
		List<SSH2ConnectionWrapper> connections = new ArrayList<SSH2ConnectionWrapper>();
		connections.add(createUsernamePasswordConnection(addresses.get(0), null));
		SSH2ConnectionWrapper credentials_to_be_copied = same_credentials_for_all ? connections.get(0) : null;

		for (int i = 1; i < addresses.size(); i++) {
			connections.add(createUsernamePasswordConnection(addresses.get(i), credentials_to_be_copied));
		}
		return connections;
	}

	public List<SSH2ConnectionWrapper> createPublicKeyConnections(List<InetAddress> addresses, boolean same_credentials_for_all) throws IOException {

		List<SSH2ConnectionWrapper> connections = new ArrayList<SSH2ConnectionWrapper>();
		connections.add(createPublicKeyConnection(addresses.get(0), null));
		SSH2ConnectionWrapper credentials_to_be_copied = same_credentials_for_all ? connections.get(0) : null;
		
		for (int i = 1; i < addresses.size(); i++) {
			connections.add(createPublicKeyConnection(addresses.get(i), credentials_to_be_copied));
		}
		return connections;
	}

	public List<HostDescriptor> createHostDescriptorsWithoutLibInstallation(List<SSH2ConnectionWrapper> connections, List<ClassPath> class_paths) throws UnequalArrayLengthsException, SSH2Exception {
		
		checkEqualLengths(connections, class_paths);
		
		List<HostDescriptor> node_descriptors = new ArrayList<HostDescriptor>();
		
		int i = 0;
		for (SSH2ConnectionWrapper connection : connections) {

			node_descriptors.add(new HostDescriptor(0, connection, class_paths.get(i)));
			i++;
		}
		return node_descriptors;
	}

	public List<HostDescriptor> createHostDescriptorsWithLibInstallation(List<SSH2ConnectionWrapper> connections, URL[] lib_urls) throws UnequalArrayLengthsException, SSH2Exception {
		
		List<HostDescriptor> node_descriptors = new ArrayList<HostDescriptor>();		
		
		int i = 0;
		for (SSH2ConnectionWrapper connection : connections) {

			node_descriptors.add(new HostDescriptor(0, connection, lib_urls));
			i++;
		}

		return node_descriptors;
	}

	private static SSH2ConnectionWrapper createUsernamePasswordConnection(InetAddress address, SSH2ConnectionWrapper credentials_to_be_copied) throws IOException {
		
		if (credentials_to_be_copied == null) {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.print("enter remote username for " + address.getCanonicalHostName() + ": ");
			String username = reader.readLine();
				
			String password = MaskedStringInput.getMaskedString("enter remote password");
		
			return new SSH2ConnectionWrapper(address, username, password);
		}
		else {
			
			return new SSH2ConnectionWrapper(address, credentials_to_be_copied.getUserName(), credentials_to_be_copied.getPassword());
		}
	}
	
	private static SSH2ConnectionWrapper createPublicKeyConnection(InetAddress address, SSH2ConnectionWrapper credentials_to_be_copied) throws IOException {
		
		if (credentials_to_be_copied == null) {
		
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.print("enter remote username for " + address.getCanonicalHostName() + ": ");
			String username = reader.readLine();
			
			String private_key_file_path = new File(new File(System.getProperty("user.home"), ".ssh"), "id_rsa").getAbsolutePath();
			String pass_phrase = MaskedStringInput.getMaskedString("enter SSH passphrase");
		
			return new SSH2ConnectionWrapper(address, username, private_key_file_path, pass_phrase);
		}
		else {
			
			return new SSH2ConnectionWrapper(address, credentials_to_be_copied.getUserName(), credentials_to_be_copied.getKeyFile(), credentials_to_be_copied.getKeyPassword());
		}
	}
	
	private static void checkEqualLengths(List<?>... lists) throws UnequalArrayLengthsException {

		int first_length = lists[0].size();
		
		for (List<?> list : lists) {
			if (list.size() != first_length) throw new UnequalArrayLengthsException();
		}
	}
}
