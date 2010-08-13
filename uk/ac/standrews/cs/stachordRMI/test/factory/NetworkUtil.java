package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.MaskedStringInput;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;

public class NetworkUtil {

	private static Map<InetAddress, SSH2ConnectionWrapper> password_connection_cache =   new HashMap<InetAddress, SSH2ConnectionWrapper>();
	private static Map<InetAddress, SSH2ConnectionWrapper> public_key_connection_cache = new HashMap<InetAddress, SSH2ConnectionWrapper>();
	
	public static SSH2ConnectionWrapper[] createUsernamePasswordConnections(InetAddress[] addresses) throws IOException {
		
		SSH2ConnectionWrapper[] connections = new SSH2ConnectionWrapper[addresses.length];
		for (int i = 0; i < addresses.length; i++) {
			connections[i] = createUsernamePasswordConnection(addresses[i]);
		}
		return connections;
	}

	public static SSH2ConnectionWrapper[] createPublicKeyConnections(InetAddress[] addresses) throws IOException {

		SSH2ConnectionWrapper[] connections = new SSH2ConnectionWrapper[addresses.length];
		for (int i = 0; i < addresses.length; i++) {
			connections[i] = createPublicKeyConnection(addresses[i]);
		}
		return connections;
	}

	public static NodeDescriptor[] createNodeDescriptors(SSH2ConnectionWrapper[] connections, String[] java_versions, ClassPath[] class_paths) throws UnequalArrayLengthsException {
		
		checkEqualLengths(connections, java_versions, class_paths);
		
		NodeDescriptor[] node_descriptors = new NodeDescriptor[connections.length];
		for (int i = 0; i < connections.length; i++) {
			node_descriptors[i] = new NodeDescriptor(connections[i], java_versions[i], class_paths[i]);
		}
		return node_descriptors;
	}

	public static NodeDescriptor[] createNodeDescriptors(SSH2ConnectionWrapper[] connections, String[] java_versions, URL[] lib_urls, File[] wget_paths, File[] lib_install_dirs) throws UnequalArrayLengthsException {
		
		checkEqualLengths(connections, java_versions, wget_paths, lib_install_dirs);
		
		NodeDescriptor[] node_descriptors = new NodeDescriptor[connections.length];
		for (int i = 0; i < connections.length; i++) {
			node_descriptors[i] = new NodeDescriptor(connections[i], java_versions[i], lib_urls, wget_paths[i], lib_install_dirs[i]);
		}
		return node_descriptors;
	}

	private static SSH2ConnectionWrapper createUsernamePasswordConnection(InetAddress address) throws IOException {
		
		SSH2ConnectionWrapper wrapper = password_connection_cache.get(address);
		
		if (wrapper != null) return wrapper;
		else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.print("enter remote username for " + address.getCanonicalHostName() + ": ");
			String username = reader.readLine();
				
			String password = MaskedStringInput.getMaskedString("enter remote password");
		
			wrapper = new SSH2ConnectionWrapper(address, username, password);
			password_connection_cache.put(address, wrapper);
			return wrapper;
		}
	}
	
	private static SSH2ConnectionWrapper createPublicKeyConnection(InetAddress address) throws IOException {
		
		SSH2ConnectionWrapper wrapper = public_key_connection_cache.get(address);
		
		if (wrapper != null) return wrapper;
		else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.print("enter remote username for " + address.getCanonicalHostName() + ": ");
			String username = reader.readLine();
			
			String private_key_file_path = new File(new File(System.getProperty("user.home"), ".ssh"), "id_rsa").getAbsolutePath();
			String pass_phrase = MaskedStringInput.getMaskedString("enter SSH passphrase");
		
			wrapper = new SSH2ConnectionWrapper(address, username, private_key_file_path, pass_phrase);
			public_key_connection_cache.put(address, wrapper);
			return wrapper;
		}
	}
	
	private static void checkEqualLengths(Object[]... arrays) throws UnequalArrayLengthsException {

		int first_length = arrays[0].length;
		
		for (Object[] array : arrays) {
			if (array.length != first_length) throw new UnequalArrayLengthsException();
		}
	}
}
