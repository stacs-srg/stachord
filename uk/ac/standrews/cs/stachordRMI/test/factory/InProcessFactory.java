package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.rmi.NotBoundException;

/**
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class InProcessFactory implements INetworkFactory {

	public INetwork makeNetwork(int number_of_nodes, String network_type) throws IOException, NotBoundException {

		return new InProcessNetwork(number_of_nodes, network_type);
	}
}
