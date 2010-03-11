package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Random;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;

public abstract class AbstractNetworkFactory {

	protected static int FIRST_NODE_PORT = 54446;
	protected static final String LOCAL_HOST = "localhost";
	
	private static Random random = new Random(FIRST_NODE_PORT);
	
	public abstract INetwork makeNetwork(int number_of_nodes) throws RemoteException, P2PNodeException, IOException;

	// Generates a random port such that lower <= port < upper.
	protected int randomPort(int lower, int upper) {
		
		return lower + random.nextInt(upper - lower);
	}
}