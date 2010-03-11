package uk.ac.standrews.cs.stachordRMI.test.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetworkFactory;
import uk.ac.standrews.cs.stachordRMI.test.util.RingIntegrityLogic;

public abstract class RoutingTests {
	
	protected INetworkFactory network_factory;
	
	private static final int[] RING_SIZES = {1,2,3,4,6,10,20};

	private static final long LOOKUP_RETRY_INTERVAL = 2000;
	
	@Before
	public void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
	}
	
	@Test
	public void routing() throws P2PNodeException, IOException {

		for (int ring_size : RING_SIZES) {
			
			Diagnostic.trace("testing routing for ring size: " + ring_size);
			routing(ring_size);
		}
	}
	
	private void routing(int ring_size) throws P2PNodeException, IOException {

		INetwork network = network_factory.makeNetwork(ring_size);
		RingIntegrityLogic.waitForStableNetwork(network.getNodes());
		
		checkRouting(network.getNodes());
		network.killAllNodes();
	}
	
	public static void checkRouting(SortedSet<IChordRemote> nodes) throws RemoteException {

		for (IChordRemote node1 : nodes) {
			for (IChordRemote node2 : nodes) {
				checkRouting(node1, node2, nodes.size());
			}
		}
	}

	private static void checkRouting(IChordRemote source, IChordRemote expected_target, int ring_size) throws RemoteException {

		// Check that a slightly smaller key than the target's key routes to the node.
		assertEquals(expected_target.getKey(),
				lookupWithRetry(source, new Key(expected_target.getKey().keyValue().subtract(BigInteger.ONE))).getKey());

		// Check that the target's own key routes to the target.
		assertEquals(expected_target.getKey(), lookupWithRetry(source, expected_target.getKey()).getKey());

		// Check that a slightly larger key than the node's key doesn't route to the node,
		// except when there is only one node, when it should do.
		IChordRemote result_for_larger_key = lookupWithRetry(source, new Key(expected_target.getKey().keyValue().add(BigInteger.ONE)));

		if (ring_size == 1) assertEquals (expected_target.getKey(), result_for_larger_key.getKey());
		else                assertNotSame(expected_target.getKey(), result_for_larger_key.getKey());
	}

	private static IChordRemote lookupWithRetry(IChordRemote source, IKey key) {
		
		while (true) {
			try {
				return source.lookup(key).getRemote();
			}
			catch (RemoteException e) {
				try { Thread.sleep(LOOKUP_RETRY_INTERVAL); }
				catch (InterruptedException e1) {}
			}
		}
	}
}
