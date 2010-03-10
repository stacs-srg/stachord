package uk.ac.standrews.cs.stachordRMI.test.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetworkFactory;
import uk.ac.standrews.cs.stachordRMI.test.util.RingIntegrityLogic;

public abstract class RingIntegrityTests {
	
	protected INetworkFactory network_factory;
	
	private static final int[] RING_SIZES = {1,2,3,4,6,10};
	
	@Before
	public void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.FULL);		
	}
	
	@Test
	public void ringStabilises() throws P2PNodeException, IOException {

		for (int ring_size : RING_SIZES) {
			
			Diagnostic.trace("testing stabilization for ring size: " + ring_size);
			ringStabilises(ring_size);
		}
	}
	
	private void ringStabilises(int ring_size) throws P2PNodeException, IOException {

		INetwork network = network_factory.makeNetwork(ring_size);
		waitForStableRing(network.getNodes());
		network.killAllNodes();
	}

	private void waitForStableRing(SortedSet<IChordRemote> nodes) {

		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "WAITING FOR RING TO STABILIZE::::::");
		RingIntegrityLogic.waitForStableNetwork(nodes);
	}
	
	@Test
	public void fingersConsistent() throws P2PNodeException, IOException {

		for (int ring_size : RING_SIZES) {
			
			Diagnostic.trace("testing finger tables for ring size: " + ring_size);
			assertTrue(fingersConsistent(ring_size));
		}
	}
	
	private boolean fingersConsistent(int ring_size) throws P2PNodeException, IOException {

		INetwork network = network_factory.makeNetwork(ring_size);
		waitForStableRing(network.getNodes());
		
		// How long to wait for finger tables to be built?
		try { Thread.sleep(10000); }
		catch (InterruptedException e) {}
		
		boolean consistent = RingIntegrityLogic.checkFingersConsistent(network.getNodes());
		network.killAllNodes();
		return consistent;
	}
}
