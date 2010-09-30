package uk.ac.standrews.cs.stachord.impl;

import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class ChordNodeFactory {

	public static IChordRemoteReference bindToNode(InetSocketAddress node_address) throws RemoteException, NotBoundException, AccessException {
	
		Registry registry = LocateRegistry.getRegistry(node_address.getHostName(), node_address.getPort());  // This doesn't make a remote call.
		IChordRemote node = (IChordRemote) registry.lookup(ChordNodeImpl.CHORD_REMOTE_SERVICE_NAME);
		
		return new ChordRemoteReference(node.getKey(), node);
	}
	
	public static IChordNode createNode(InetSocketAddress local_address) throws RemoteException, NotBoundException {
		return new ChordNodeImpl(local_address);
	}

	public static IChordNode createNode(InetSocketAddress local_address, InetSocketAddress known_node_address) throws RemoteException, NotBoundException {
		return new ChordNodeImpl(local_address, known_node_address);
	}

	public static IChordNode createNode(InetSocketAddress local_address, InetSocketAddress known_node_address, IKey key) throws RemoteException, NotBoundException {
		return new ChordNodeImpl(local_address, known_node_address, key);
	}
}
