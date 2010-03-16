package uk.ac.standrews.cs.stachordRMI.test.ringIntegrity;

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

public abstract class RingIntegrityTests {
	
	protected INetworkFactory network_factory;
	
	private static final int[] RING_SIZES = {1,2,3,4,6,10,20};
	
	@Before
	public void setUp() throws Exception {
			
		Diagnostic.setLevel(DiagnosticLevel.NONE);
		ChordNodeImpl.setTestMode(true);                 // Cause hard fault in case of failures, since there shouldn't be any.
	}
	
	@Test
	public void ringBecomesStable() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println();
			Diagnostic.trace("testing stabilization for ring size: " + ring_size);
			
			ringBecomesStable(ring_size, AbstractNetworkFactory.RANDOM);
			ringBecomesStable(ring_size, AbstractNetworkFactory.EVEN);
			ringBecomesStable(ring_size, AbstractNetworkFactory.CLUSTERED);
		}
	}
	
	@Test
	public void fingerTablesBecomeComplete() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println();
			Diagnostic.trace("testing finger tables for ring size: " + ring_size);
			
			fingerTablesBecomeComplete(ring_size, AbstractNetworkFactory.RANDOM);
			fingerTablesBecomeComplete(ring_size, AbstractNetworkFactory.EVEN);
			fingerTablesBecomeComplete(ring_size, AbstractNetworkFactory.CLUSTERED);
		}
	}
	
	@Test
	public void successorListsBecomeComplete() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println();
			Diagnostic.trace("testing successor lists for ring size: " + ring_size);
			
			successorListsBecomeComplete(ring_size, AbstractNetworkFactory.RANDOM);
			successorListsBecomeComplete(ring_size, AbstractNetworkFactory.EVEN);
			successorListsBecomeComplete(ring_size, AbstractNetworkFactory.CLUSTERED);
		}
	}
	
	private void ringBecomesStable(int ring_size, String network_type) throws IOException, NotBoundException {

		INetwork network = network_factory.makeNetwork(ring_size, network_type);
		TestLogic.waitForStableRing(network.getNodes());
		network.killAllNodes();
	}

	private void fingerTablesBecomeComplete(int ring_size, String network_type) throws IOException, NotBoundException {

		INetwork network = network_factory.makeNetwork(ring_size, network_type);
		
		TestLogic.waitForStableRing(network.getNodes());		
		TestLogic.waitForCompleteFingerTables(network.getNodes());
		
		network.killAllNodes();
	}	
	
	private void successorListsBecomeComplete(int ring_size, String network_type) throws IOException, NotBoundException {
		
		INetwork network = network_factory.makeNetwork(ring_size, network_type);
		
		TestLogic.waitForStableRing(network.getNodes());
		TestLogic.waitForCompleteSuccessorLists(network.getNodes());
		
		network.killAllNodes();
	}
}
