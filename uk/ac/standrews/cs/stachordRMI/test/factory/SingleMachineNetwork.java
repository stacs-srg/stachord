package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.List;

import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.Processes;
import uk.ac.standrews.cs.stachordRMI.servers.AbstractServer;

import com.mindbright.ssh2.SSH2Exception;

/**
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class SingleMachineNetwork extends MultipleMachineNetwork {

	static final String LOCAL_HOST = "localhost";

	public SingleMachineNetwork(int number_of_nodes, String network_type) throws IOException, NotBoundException {
		
		try {
			init(new NodeDescriptor[number_of_nodes], network_type);
		}
		catch (SSH2Exception e) {
			ErrorHandling.hardExceptionError(e, "unexpected SSH error on local network creation");
		}
	}

	protected Process runProcess(NodeDescriptor node_descriptor, Class<? extends AbstractServer> node_class, List<String> args) throws IOException, SSH2Exception {
		
		// Ignore node_descriptor for local process.
		return Processes.runJavaProcess(node_class, args);
	}
	
	protected String getHost(NodeDescriptor node_descriptor) {
		
		return LOCAL_HOST;
	}
}
