package uk.ac.standrews.cs.stachord.test.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.mindbright.ssh2.SSH2Exception;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.MaskedStringInput;

/**
 * Provides various SSH-related utility methods.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class NetworkUtil {

	/**
	 * Creates a list of interactively entered username/password credentials for the specified addresses. Optionally the same credentials can be used for all addresses.
	 * 
	 * @param addresses the addresses
	 * @param same_credentials_for_all true if the same credentials should be used for all addresses
	 * @return a list of username/password credentials
	 * 
	 * @throws IOException if an error occurs when trying to read in a username or password
	 */
	public static List<SSH2ConnectionWrapper> createUsernamePasswordConnections(List<InetAddress> addresses, boolean same_credentials_for_all) throws IOException {
		
		List<SSH2ConnectionWrapper> connections = new ArrayList<SSH2ConnectionWrapper>();
		connections.add(createUsernamePasswordConnection(addresses.get(0), null));
		SSH2ConnectionWrapper credentials_to_be_copied = same_credentials_for_all ? connections.get(0) : null;

		for (int i = 1; i < addresses.size(); i++) {
			connections.add(createUsernamePasswordConnection(addresses.get(i), credentials_to_be_copied));
		}
		return connections;
	}

	/**
	 * Creates a list of interactively entered username/public key credentials for the specified addresses. Optionally the same credentials can be used for all addresses.
	 * 
	 * @param addresses the addresses
	 * @param same_credentials_for_all true if the same credentials should be used for all addresses
	 * @return a list of username/password credentials
	 * 
	 * @throws IOException if an error occurs when trying to read in a username or password
	 */
	public static List<SSH2ConnectionWrapper> createPublicKeyConnections(List<InetAddress> addresses, boolean same_credentials_for_all) throws IOException {

		List<SSH2ConnectionWrapper> connections = new ArrayList<SSH2ConnectionWrapper>();
		connections.add(createPublicKeyConnection(addresses.get(0), null));
		SSH2ConnectionWrapper credentials_to_be_copied = same_credentials_for_all ? connections.get(0) : null;
		
		for (int i = 1; i < addresses.size(); i++) {
			connections.add(createPublicKeyConnection(addresses.get(i), credentials_to_be_copied));
		}
		return connections;
	}

	/**
	 * Creates a list of host descriptors given a list of SSH credentials and corresponding class paths.
	 * 
	 * @param connections the SSH credentials
	 * @param class_paths the corresponding class paths
	 * @return a list of host descriptors
	 * 
	 * @throws UnequalArrayLengthsException if the lengths of the two lists are different
	 * @throws SSH2Exception if an error occurs when attempting to contact one of the specified remote hosts
	 */
	public static List<HostDescriptor> createHostDescriptors(List<SSH2ConnectionWrapper> connections, List<ClassPath> class_paths) throws UnequalArrayLengthsException, SSH2Exception {
		
		checkEqualLengths(connections, class_paths);
		
		List<HostDescriptor> node_descriptors = new ArrayList<HostDescriptor>();
		
		int i = 0;
		for (SSH2ConnectionWrapper connection : connections) {

			node_descriptors.add(new HostDescriptor(connection, 0, class_paths.get(i)));
			i++;
		}
		return node_descriptors;
	}

	/**
	 * Creates a list of host descriptors given a list of SSH credentials and a list of URLs from which class path entries can be obtained.
	 * 
	 * @param connections the SSH credentials
	 * @param lib_urls the class path entry URLs
	 * @return a list of host descriptors
	 * 
	 * @throws SSH2Exception if an error occurs when attempting to contact one of the specified remote hosts
	 */
	public static List<HostDescriptor> createHostDescriptors(List<SSH2ConnectionWrapper> connections, URL[] lib_urls) throws SSH2Exception {
		
		List<HostDescriptor> node_descriptors = new ArrayList<HostDescriptor>();		
		
		int i = 0;
		for (SSH2ConnectionWrapper connection : connections) {

			node_descriptors.add(new HostDescriptor(connection, 0, lib_urls));
			i++;
		}

		return node_descriptors;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static SSH2ConnectionWrapper createUsernamePasswordConnection(InetAddress address, SSH2ConnectionWrapper credentials_to_be_copied) throws IOException {
		
		if (credentials_to_be_copied == null) {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			System.out.print("enter remote username for " + address.getCanonicalHostName() + ": ");
			String username = reader.readLine();
				
			String password = MaskedStringInput.getMaskedString("enter remote password");
		
			return new SSH2ConnectionWrapper(address, username, password);
		}
		
		return new SSH2ConnectionWrapper(address, credentials_to_be_copied.getUserName(), credentials_to_be_copied.getPassword());
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
		
		return new SSH2ConnectionWrapper(address, credentials_to_be_copied.getUserName(), credentials_to_be_copied.getKeyFile(), credentials_to_be_copied.getKeyPassphrase());
	}
	
	private static void checkEqualLengths(List<?>... lists) throws UnequalArrayLengthsException {

		int first_length = lists[0].size();
		
		for (List<?> list : lists) {
			if (list.size() != first_length) throw new UnequalArrayLengthsException();
		}
	}
}
