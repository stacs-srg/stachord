package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.math.BigInteger;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Random;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

public abstract class AbstractNetworkFactory {
	
	// TODO check for ports already in use.
	
	public static final String RANDOM = "RANDOM";          // Nodes randomly distributed around the ring.
	public static final String EVEN = "EVEN";              // Nodes evenly distributed around the ring.
	public static final String CLUSTERED = "CLUSTERED";    // Nodes clustered tightly in one region of the ring.

	static int FIRST_NODE_PORT = 54446;
	static final String LOCAL_HOST = "localhost";
	
	private static Random random = new Random(FIRST_NODE_PORT);
	
	public abstract INetwork makeNetwork(int number_of_nodes, String network_type) throws RemoteException, IOException, NotBoundException;

	// Generates a random port such that lower <= port < upper.
	protected int randomPort(int lower, int upper) {
		
		return lower + random.nextInt(upper - lower);
	}

	protected IKey[] generateNodeKeys(String network_type, int number_of_nodes) {
		
		IKey[] node_keys = new IKey[number_of_nodes];
		
		// Leave null entries for RANDOM since the keys will be generated from hashing addresses.
		if (!network_type.equals(RANDOM)) {
		
			BigInteger key_value = null;
			BigInteger node_increment = null;
	
			if (network_type.equals(EVEN)) {
				
				key_value = new BigInteger("0");
				node_increment = Key.KEYSPACE_SIZE.divide(new BigInteger(String.valueOf(number_of_nodes)));	
			}
			
			if (network_type.equals(CLUSTERED)) {
				
				// Span the wrap-over point just to be awkward.
				key_value = Key.KEYSPACE_SIZE.subtract(new BigInteger(String.valueOf(number_of_nodes / 2)));
				node_increment = new BigInteger("1");
			}
			
			for (int i = 0; i < number_of_nodes; i++) {
				node_keys[i] = new Key(key_value);
				key_value = key_value.add(node_increment);
			}
		}
		
//		System.out.println("network type: " + network_type);
//		System.out.println("keys:");
//		for (int i = 0; i < number_of_nodes; i++) System.out.println(node_keys[i]);
		
		return node_keys;
	}
}