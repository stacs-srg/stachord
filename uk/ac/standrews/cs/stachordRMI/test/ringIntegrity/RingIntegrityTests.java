package uk.ac.standrews.cs.stachordRMI.test.ringIntegrity;

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

public abstract class RingIntegrityTests {
	
	private static final int[] RING_SIZES = {1,2,3,4,6,10,20};
	
	@Before
	public void setUp() throws Exception {
			
		Diagnostic.setLevel(DiagnosticLevel.NONE);
	}
	
	@Test
	public void ringBecomesStable() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println();
			Diagnostic.trace("testing stabilization for ring size: " + ring_size);
			
			ringBecomesStable(ring_size, MultipleMachineNetwork.RANDOM);
			ringBecomesStable(ring_size, MultipleMachineNetwork.EVEN);
			ringBecomesStable(ring_size, MultipleMachineNetwork.CLUSTERED);
		}
	}
	
	@Test
	public void fingerTablesBecomeComplete() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println();
			Diagnostic.trace("testing finger tables for ring size: " + ring_size);
			
			fingerTablesBecomeComplete(ring_size, MultipleMachineNetwork.RANDOM);
			fingerTablesBecomeComplete(ring_size, MultipleMachineNetwork.EVEN);
			fingerTablesBecomeComplete(ring_size, MultipleMachineNetwork.CLUSTERED);
		}
	}
	
	@Test
	public void successorListsBecomeComplete() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println();
			Diagnostic.trace("testing successor lists for ring size: " + ring_size);
			
			successorListsBecomeComplete(ring_size, MultipleMachineNetwork.RANDOM);
			successorListsBecomeComplete(ring_size, MultipleMachineNetwork.EVEN);
			successorListsBecomeComplete(ring_size, MultipleMachineNetwork.CLUSTERED);
		}
	}
	
	private void ringBecomesStable(int ring_size, String network_type) throws IOException, NotBoundException {

		INetwork network = new SingleMachineNetwork(ring_size, network_type);
		TestLogic.waitForStableRing(network.getNodes());
		network.killAllNodes();
	}

	private void fingerTablesBecomeComplete(int ring_size, String network_type) throws IOException, NotBoundException {

		INetwork network = new SingleMachineNetwork(ring_size, network_type);
		
		TestLogic.waitForStableRing(network.getNodes());		
		TestLogic.waitForCompleteFingerTables(network.getNodes());
		
		network.killAllNodes();
	}	
	
	private void successorListsBecomeComplete(int ring_size, String network_type) throws IOException, NotBoundException {
		
		INetwork network = new SingleMachineNetwork(ring_size, network_type);
		
		TestLogic.waitForStableRing(network.getNodes());
		TestLogic.waitForCompleteSuccessorLists(network.getNodes());
		
		network.killAllNodes();
	}
}
