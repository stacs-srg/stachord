package uk.ac.standrews.cs.stachordRMI.test.routing;

import java.io.IOException;
import java.rmi.NotBoundException;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.MultipleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.SingleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.util.TestLogic;

public abstract class RoutingTests {
	
	private static final int[] RING_SIZES = {1,2,3,4,6,10,20};

	@Before
	public void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
	}
	
	@Test
	public void routingBecomesCorrect() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println();
			Diagnostic.trace("testing routing for ring size: " + ring_size);
			
			routingBecomesCorrect(ring_size, MultipleMachineNetwork.RANDOM);
			routingBecomesCorrect(ring_size, MultipleMachineNetwork.EVEN);
			routingBecomesCorrect(ring_size, MultipleMachineNetwork.CLUSTERED);
		}
	}
	
	private void routingBecomesCorrect(int ring_size, String network_type) throws IOException, NotBoundException {

		INetwork network = new SingleMachineNetwork(ring_size, network_type);
		
		TestLogic.waitForStableRing(network.getNodes());
		TestLogic.waitForCorrectRouting(network.getNodes());
		
		network.killAllNodes();
	}
}
