package uk.ac.standrews.cs.stachord.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.ProcessInvocation;
import uk.ac.standrews.cs.nds.remote_management.UnknownPlatformException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.servers.StartNodeInNewRing;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Provides methods for creating new Chord nodes and binding to existing remote Chord nodes.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class ChordNodeFactory {

	private static final int REGISTRY_RETRY_INTERVAL =    2000;       // Retry connecting to remote nodes at 2s intervals.
	private static final int REGISTRY_TIMEOUT_INTERVAL = 20000;       // Give up after 20s.
	
	private static int next_port = 54496;                             // The next port to be used; static to allow multiple concurrent networks.
	private static final Object sync = new Object();                  // Used for serializing network creation.
	
	/**
	 * Creates a new Chord node running at a given local network address on a given port, establishing a new one-node ring.
	 * 
	 * @param local_address the local address of the node
	 * @return the new node
	 * @throws RemoteException if an error occurs in making the new node accessible for remote access
	 */
	public static IChordNode createLocalNode(InetSocketAddress local_address) throws RemoteException {
		return new ChordNodeImpl(local_address);
	}

	/**
	 * Creates a new Chord node running at a given local network address on a given port, with a given key, establishing a new one-node ring.
	 * 
	 * @param local_address the local address of the node
	 * @param key the key of the new node
	 * @return the new node
	 * @throws RemoteException if an error occurs in making the new node accessible for remote access
	 */
	public static IChordNode createLocalNode(InetSocketAddress local_address, IKey key) throws RemoteException {
		return new ChordNodeImpl(local_address, key);
	}

	/**
	 * Creates a new Chord node running at a given remote network address on a given port, establishing a new one-node ring.
	 * 
	 * @param host_descriptor a structure containing access details for a remote host
	 * @return a remote reference to the new Chord node
	 * 
	 * @throws IOException if an error occurs when reading communicating with the remote host
	 * @throws SSH2Exception if an SSH connection to the remote host cannot be established
	 * @throws TimeoutException if the node cannot be instantiated within the timeout period
	 * @throws UnknownPlatformException if the operating system of the remote host cannot be established
	 */
	public static IChordRemoteReference createRemoteNode(HostDescriptor host_descriptor) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException  {

		return createRemoteNode(host_descriptor, null);
	}

	/**
	 * Creates a new Chord node running at a given remote network address on a given port, establishing a new one-node ring.
	 * 
	 * @param host_descriptor a structure containing access details for a remote host
	 * @param key the key of the new node
	 * @return a remote reference to the new Chord node
	 * 
	 * @throws IOException if an error occurs when reading communicating with the remote host
	 * @throws SSH2Exception if an SSH connection to the remote host cannot be established
	 * @throws TimeoutException if the node cannot be instantiated within the timeout period
	 * @throws UnknownPlatformException if the operating system of the remote host cannot be established
	 */
	public static IChordRemoteReference createRemoteNode(HostDescriptor host_descriptor, IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException  {

		instantiateRemoteNode(host_descriptor, key);
		return bindToRemoteNodeWithRetry(NetworkUtil.getInetSocketAddress(host_descriptor.host, host_descriptor.port));
	}

	/**
	 * Creates a new node on a given host, establishing a new one-node ring.
	 * 
	 * @param host_descriptor a structure containing access details for a remote host
	 * @return a process handle for the new node
	 * 
	 * @throws IOException if an error occurs when reading communicating with the remote host
	 * @throws SSH2Exception if an SSH connection to the remote host cannot be established
	 * @throws TimeoutException if the node cannot be instantiated within the timeout period
	 * @throws UnknownPlatformException if the operating system of the remote host cannot be established
	 */
	public static Process instantiateRemoteNode(HostDescriptor host_descriptor) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

		return instantiateRemoteNode(host_descriptor, null);
	}

	/**
	 * Creates a new node on a given host, establishing a new one-node ring.
	 * 
	 * @param host_descriptor a structure containing access details for a remote host
	 * @param key the key of the new node
	 * @return a process handle for the new node
	 * 
	 * @throws IOException if an error occurs when reading communicating with the remote host
	 * @throws SSH2Exception if an SSH connection to the remote host cannot be established
	 * @throws TimeoutException if the node cannot be instantiated within the timeout period
	 * @throws UnknownPlatformException if the operating system of the remote host cannot be established
	 */
	public static Process instantiateRemoteNode(HostDescriptor host_descriptor, IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {

		List<String> args = new ArrayList<String>();
		
		args.add("-s" + NetworkUtil.formatHostAddress(host_descriptor.host, host_descriptor.port));
		if (key != null) addKeyArg(key, args);

		return ProcessInvocation.runJavaProcess(StartNodeInNewRing.class, args, host_descriptor);
	}



	public static void createAndBindToRemoteNodeOnFreePort(final HostDescriptor host_descriptor, final IKey key) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {
		
		ArgGen arg_gen = new ArgGen() {
			
			public List<String> getArgs(int local_port) {
				
				List<String> args = new ArrayList<String>();
				
				args.add("-s" + NetworkUtil.formatHostAddress(host_descriptor.host, local_port));
				addKeyArg(key, args);
				
				return args;
			}
		};
		
		createAndBindToRemoteNodeOnFreePort(host_descriptor, arg_gen, StartNodeInNewRing.class);
	}



	/**
	 * Binds to an existing remote Chord node running at a given network address.
	 * 
	 * @param node_address the address of the existing node
	 * @return a remote reference to the node
	 * @throws RemoteException if an error occurs communicating with the remote machine
	 * @throws NotBoundException if the Chord node is not accessible with the expected service name
	 */
	public static IChordRemoteReference bindToRemoteNode(InetSocketAddress node_address) throws RemoteException, NotBoundException {
	
		Registry registry = LocateRegistry.getRegistry(node_address.getHostName(), node_address.getPort());  // This doesn't make a remote call.
		IChordRemote node = (IChordRemote) registry.lookup(ChordNodeImpl.CHORD_REMOTE_SERVICE_NAME);
		
		return new ChordRemoteReference(node.getKey(), node);
	}

	public static IChordRemoteReference bindToRemoteNodeWithRetry(InetSocketAddress node_address) throws TimeoutException {
				
		long start_time = System.currentTimeMillis();
		
		while (true) {
	
			try {
				return bindToRemoteNode(node_address);
			}
			catch (RemoteException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "registry location failed: " + e.getMessage());
			}
			catch (NotBoundException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "binding to node in registry failed");
			}
			catch (Exception e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "registry lookup failed");
			}
			
			try {
				Thread.sleep(REGISTRY_RETRY_INTERVAL);
			}
			catch (InterruptedException e) {
			}
			
			long duration = System.currentTimeMillis() - start_time;
			if (duration > REGISTRY_TIMEOUT_INTERVAL) throw new TimeoutException();
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static void createAndBindToRemoteNodeOnFreePort(HostDescriptor host_descriptor, ArgGen arg_gen, Class<?> clazz) throws IOException, SSH2Exception, TimeoutException, UnknownPlatformException {
		
		boolean finished = false;
		
		while (!finished) {
			
			int port = 0;
			
			synchronized (sync) {
				port = next_port++;
			}
	
			host_descriptor.port = port;
		
			List<String> args = arg_gen.getArgs(port);
			
			host_descriptor.process = ProcessInvocation.runJavaProcess(clazz, args, host_descriptor);
			host_descriptor.application_reference = bindToRemoteNodeWithRetry(NetworkUtil.getInetSocketAddress(host_descriptor.host, host_descriptor.port));
			finished = true;
		}
	}
	
	private static void addKeyArg(IKey key, List<String> args) {
	
		if (key != null) args.add("-x" + key.toString(Key.DEFAULT_RADIX)); 
	}

	private interface ArgGen {
		List<String> getArgs(int local_port);
	}
}