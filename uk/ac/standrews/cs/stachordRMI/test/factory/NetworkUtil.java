package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;

import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.MaskedStringInput;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.remote_management.infrastructure.MachineDescriptor;

public class NetworkUtil {

	public static SSH2ConnectionWrapper[] createUsernamePasswordConnections(InetAddress[] addresses, boolean same_credentials_for_all) throws IOException {
		
		SSH2ConnectionWrapper[] connections = new SSH2ConnectionWrapper[addresses.length];
		connections[0] = createUsernamePasswordConnection(addresses[0], null);

		for (int i = 1; i < addresses.length; i++) {
			connections[i] = createUsernamePasswordConnection(addresses[i], same_credentials_for_all ? connections[i-1] : null);
		}
		return connections;
	}

	public static SSH2ConnectionWrapper[] createPublicKeyConnections(InetAddress[] addresses, boolean same_credentials_for_all) throws IOException {

		SSH2ConnectionWrapper[] connections = new SSH2ConnectionWrapper[addresses.length];
		connections[0] = createPublicKeyConnection(addresses[0], null);
		
		for (int i = 1; i < addresses.length; i++) {
			connections[i] = createPublicKeyConnection(addresses[i], same_credentials_for_all ? connections[i-1] : null);
		}
		return connections;
	}

	public static MachineDescriptor[] createNodeDescriptors(SSH2ConnectionWrapper[] connections, String[] java_versions, ClassPath[] class_paths) throws UnequalArrayLengthsException {
		
		checkEqualLengths(connections, java_versions, class_paths);
		
		MachineDescriptor[] node_descriptors = new MachineDescriptor[connections.length];
		for (int i = 0; i < connections.length; i++) {
			node_descriptors[i] = new MachineDescriptor(connections[i].getServer().getCanonicalHostName(), connections[i], java_versions[i], class_paths[i]);
		}
		return node_descriptors;
	}

	public static MachineDescriptor[] createNodeDescriptors(SSH2ConnectionWrapper[] connections, String[] java_versions, URL[] lib_urls, File[] wget_paths, File[] lib_install_dirs) throws UnequalArrayLengthsException {
		
		checkEqualLengths(connections, java_versions, wget_paths, lib_install_dirs);
		
		MachineDescriptor[] node_descriptors = new MachineDescriptor[connections.length];
		for (int i = 0; i < connections.length; i++) {
			node_descriptors[i] = new MachineDescriptor(connections[i].getServer().getCanonicalHostName(), connections[i], java_versions[i], lib_urls, wget_paths[i].getAbsolutePath(), lib_install_dirs[i].getAbsolutePath());
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
	
	private static void checkEqualLengths(Object[]... arrays) throws UnequalArrayLengthsException {

		int first_length = arrays[0].length;
		
		for (Object[] array : arrays) {
			if (array.length != first_length) throw new UnequalArrayLengthsException();
		}
	}
}
