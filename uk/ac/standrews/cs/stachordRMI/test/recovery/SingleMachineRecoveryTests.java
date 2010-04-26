package uk.ac.standrews.cs.stachordRMI.test.recovery;


import java.io.IOException;
import java.rmi.NotBoundException;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.test.factory.MultipleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.SingleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.util.TestLogic;

public class SingleMachineRecoveryTests {
	
	private static final int[] RING_SIZES = {2,3,4,5};

	@Before
	public void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
	}
	
	@Test
	public void ringRecoversRandom() throws IOException, NotBoundException {
			
		ringRecovers(MultipleMachineNetwork.RANDOM);
	}
	
	@Test
	public void ringRecoversEven() throws IOException, NotBoundException {
		
		ringRecovers(MultipleMachineNetwork.EVEN);
	}

	@Test
	public void ringRecoversClustered() throws IOException, NotBoundException {
		
		ringRecovers(MultipleMachineNetwork.CLUSTERED);
	}

	private void ringRecovers(String network_type) throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println("testing recovery for ring size: " + ring_size + ", network type: " + network_type);
			
			ringRecovers(ring_size, network_type);
		}
	}
	
	private void ringRecovers(int ring_size, String network_type) throws IOException, NotBoundException {
		
		TestLogic.ringRecovers(new SingleMachineNetwork(ring_size, network_type));
	}
}
