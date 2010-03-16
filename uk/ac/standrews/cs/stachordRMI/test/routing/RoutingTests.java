package uk.ac.standrews.cs.stachordRMI.test.routing;

import java.io.IOException;
import java.rmi.NotBoundException;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.test.factory.AbstractNetworkFactory;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetworkFactory;
import uk.ac.standrews.cs.stachordRMI.test.util.TestLogic;

public abstract class RoutingTests {
	
	protected INetworkFactory network_factory;
	
	private static final int[] RING_SIZES = {1,2,3,4,6,10,20};

	@Before
	public void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
		ChordNodeImpl.setTestMode(true);                 // Cause hard fault in case of failures, since there shouldn't be any.
	}
	
	@Test
	public void routingBecomesCorrect() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println();
			Diagnostic.trace("testing routing for ring size: " + ring_size);
			
			routingBecomesCorrect(ring_size, AbstractNetworkFactory.RANDOM);
			routingBecomesCorrect(ring_size, AbstractNetworkFactory.EVEN);
			routingBecomesCorrect(ring_size, AbstractNetworkFactory.CLUSTERED);
		}
	}
	
	private void routingBecomesCorrect(int ring_size, String network_type) throws IOException, NotBoundException {

		INetwork network = network_factory.makeNetwork(ring_size, network_type);
		
		TestLogic.waitForStableRing(network.getNodes());
		TestLogic.waitForCorrectRouting(network.getNodes());
		
		network.killAllNodes();
	}
}
