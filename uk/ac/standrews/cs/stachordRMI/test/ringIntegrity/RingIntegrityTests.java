package uk.ac.standrews.cs.stachordRMI.test.ringIntegrity;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetworkFactory;
import uk.ac.standrews.cs.stachordRMI.test.util.RingIntegrityLogic;

public abstract class RingIntegrityTests {
	
	protected INetworkFactory network_factory;
	
	private static final int[] RING_SIZES = {1,2,3,4,6,10,20};
	
	@Before
	public void setUp() throws Exception {
			
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
	}
	
	@Test
	public void ringStabilises() throws P2PNodeException, IOException {

		for (int ring_size : RING_SIZES) {
			
			Diagnostic.trace("testing stabilization for ring size: " + ring_size);
			ringStabilises(ring_size);
		}
	}
	
	@Test
	public void fingersConsistent() throws P2PNodeException, IOException {

		for (int ring_size : RING_SIZES) {
			
			Diagnostic.trace("testing finger tables for ring size: " + ring_size);
			fingersConsistent(ring_size);
		}
	}
	
	@Test
	public void successorsConsistent() throws P2PNodeException, IOException {

		for (int ring_size : RING_SIZES) {
			
			Diagnostic.trace("testing successor lists for ring size: " + ring_size);
			successorsConsistent(ring_size);
		}
	}
	
	private void ringStabilises(int ring_size) throws P2PNodeException, IOException {

		INetwork network = network_factory.makeNetwork(ring_size);
		RingIntegrityLogic.waitForStableNetwork(network.getNodes());
		network.killAllNodes();
	}

	private void fingersConsistent(int ring_size) throws P2PNodeException, IOException {

		INetwork network = network_factory.makeNetwork(ring_size);
		RingIntegrityLogic.waitForStableNetwork(network.getNodes());
		
		// How long to wait for finger tables to be built?
		try { Thread.sleep(10000); }
		catch (InterruptedException e) {}
		
		RingIntegrityLogic.checkFingersConsistent(network.getNodes());
		network.killAllNodes();
	}	
	
	private void successorsConsistent(int ring_size) throws P2PNodeException, IOException {
		
		INetwork network = network_factory.makeNetwork(ring_size);
		RingIntegrityLogic.waitForStableNetwork(network.getNodes());
		
		// How long to wait for successor lists to be built?
		try { Thread.sleep(10000); }
		catch (InterruptedException e) {}
		
		RingIntegrityLogic.checkSuccessorsConsistent(network.getNodes());
		network.killAllNodes();
	}
}
