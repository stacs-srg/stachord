package uk.ac.standrews.cs.stachord.impl;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Provides methods for creating new Chord nodes and binding to existing remote Chord nodes.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class ChordNodeFactory {

	/**
	 * Binds to an existing remote Chord node running at a given network address.
	 * 
	 * @param node_address the address of the existing node
	 * @return a remote reference to the node
	 * @throws RemoteException if an error occurs communicating with the remote machine
	 * @throws NotBoundException if the Chord node is not accessible with the expected service name
	 */
	public static IChordRemoteReference bindToNode(InetSocketAddress node_address) throws RemoteException, NotBoundException {
	
		Registry registry = LocateRegistry.getRegistry(node_address.getHostName(), node_address.getPort());  // This doesn't make a remote call.
		IChordRemote node = (IChordRemote) registry.lookup(ChordNodeImpl.CHORD_REMOTE_SERVICE_NAME);
		
		return new ChordRemoteReference(node.getKey(), node);
	}
	
	/**
	 * Creates a new Chord node running at a given local network address, establishing a new one-node ring.
	 * 
	 * @param local_address the local address of the node
	 * @return the new node
	 * @throws RemoteException if an error occurs in making the new node accessible for remote access
	 */
	public static IChordNode createNode(InetSocketAddress local_address) throws RemoteException {
		return new ChordNodeImpl(local_address);
	}

	/**
	 * Creates a new Chord node running at a given local network address, joining an existing ring.
	 * The key of the new node is derived by hashing its network address.
	 * 
	 * @param local_address the local address of the node
	 * @param known_node_address the address of a node in the existing ring
	 * @return the new node
	 * @throws RemoteException if an error occurs in making the new node accessible for remote access, or in communication with the remote machine
	 * @throws NotBoundException if the node in the existing ring is not accessible with the expected service name
	 */
	public static IChordNode createNode(InetSocketAddress local_address, InetSocketAddress known_node_address) throws RemoteException, NotBoundException {
		return new ChordNodeImpl(local_address, known_node_address);
	}

	/**
	 * Creates a new Chord node running at a given local network address, with a given key, joining an existing ring.
	 * 
	 * @param local_address the local address of the node
	 * @param known_node_address the address of a node in the existing ring
	 * @param key the key of the new node
	 * @return the new node
	 * @throws RemoteException if an error occurs in making the new node accessible for remote access, or in communication with the remote machine
	 * @throws NotBoundException if the node in the existing ring is not accessible with the expected service name
	 */
	public static IChordNode createNode(InetSocketAddress local_address, InetSocketAddress known_node_address, IKey key) throws RemoteException, NotBoundException {
		return new ChordNodeImpl(local_address, known_node_address, key);
	}
}
