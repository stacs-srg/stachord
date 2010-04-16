package uk.ac.standrews.cs.stachordRMI.test.factory;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.util.NodeComparator;

public abstract class AbstractNetwork implements INetwork {
	
	public static final String RANDOM = "RANDOM";          // Nodes randomly distributed around the ring.
	public static final String EVEN = "EVEN";              // Nodes evenly distributed around the ring.
	public static final String CLUSTERED = "CLUSTERED";    // Nodes clustered tightly in one region of the ring.

	private static final int MAX_PORT = 65535;

	static int FIRST_NODE_PORT = 54496;

	static final String LOCAL_HOST = "localhost";
	
	private static Random random = new Random(FIRST_NODE_PORT);
	
	IKey[] node_keys;
	int[] node_ports;
	SortedSet<IChordRemoteReference> nodes;
	
	public AbstractNetwork(int number_of_nodes, String network_type) throws SocketException {
		
		if (!network_type.equals(RANDOM) && !network_type.equals(EVEN) && !network_type.equals(CLUSTERED)) fail("unknown network type");

		node_keys = generateNodeKeys(network_type, number_of_nodes);
		node_ports = generatePorts(FIRST_NODE_PORT, number_of_nodes);
		
		nodes = new TreeSet<IChordRemoteReference>(new NodeComparator());
	}

	public SortedSet<IChordRemoteReference> getNodes() {
		
		return nodes;
	}

	protected abstract IChordRemoteReference createFirstNode(int port, IKey key) throws IOException, NotBoundException;

	protected abstract IChordRemoteReference createJoiningNode(int port, int join_port, IKey key) throws IOException, NotBoundException;

	protected void setupNetwork(int number_of_nodes) throws IOException, NotBoundException {
				
		IChordRemoteReference first = createFirstNode(node_ports[0], node_keys[0]);
		nodes.add(first);
	
		for (int port_index = 1; port_index < number_of_nodes; port_index++) {
			
			int port =      node_ports[port_index];
			int join_port = node_ports[randomPortIndex(0, port_index)];
			IKey key =      node_keys[port_index];
	
			IChordRemoteReference next = createJoiningNode(port, join_port, key);
			nodes.add(next);
		}
		
		// For next time, adjust first node port beyond the ports just used.
		FIRST_NODE_PORT = node_ports[number_of_nodes - 1] + 1;
	}
	
	// Generates a random port such that lower <= port < upper.
	protected int randomPortIndex(int lower, int upper) {
		
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
		
		return node_keys;
	}
	
	protected int[] generatePorts(int first_port, int number_of_nodes) throws SocketException {

		int[] ports = new int[number_of_nodes];
		int port = first_port;
		
		for (int i = 0; i < number_of_nodes; i++) {
			port = nextFreePort(port);
			ports[i] = port;
			port++;
		}
		
		return ports;
	}
	
	private int nextFreePort(int port) throws SocketException {

		while (!free(port)) {
			port++;
			if (port >= MAX_PORT) throw new SocketException("ran out of ports");
		}

		return port;
	}

	private boolean free(int port) {

		try {
			ServerSocket server_socket = new ServerSocket(port, 0);
			server_socket.close();
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}
}