package uk.ac.standrews.cs.stachordRMI.test.recovery;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetworkFactory;
import uk.ac.standrews.cs.stachordRMI.test.routing.RoutingTests;
import uk.ac.standrews.cs.stachordRMI.test.util.RingIntegrityLogic;

public abstract class RecoveryTests {
	
	protected INetworkFactory network_factory;
	
	private static final int[] RING_SIZES = {2,3,4,6,10,20};

	private static final double PROPORTION_TO_KILL = 0.2;

	private static final long DEATH_CHECK_INTERVAL = 2000;
	
	private static final int RANDOM_SEED = 32423545;
	
	@Before
	public void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
	}
	
	@Test
	public void recovery() throws P2PNodeException, IOException {

		for (int ring_size : RING_SIZES) {
			
			Diagnostic.trace("testing recovery for ring size: " + ring_size);
			recovery(ring_size);
		}
	}
	
	private void recovery(int ring_size) throws P2PNodeException, IOException {

		INetwork network = network_factory.makeNetwork(ring_size);
		RingIntegrityLogic.waitForStableNetwork(network.getNodes());
		
		recovery(network);
		network.killAllNodes();
	}
	
	private void recovery(INetwork network) throws P2PNodeException, IOException {

		killPartOfNetwork(network);
		
		RingIntegrityLogic.waitForStableNetwork(network.getNodes());
		RingIntegrityLogic.checkFingersConsistent(network.getNodes());
		RingIntegrityLogic.checkSuccessorsConsistent(network.getNodes());
		
		RoutingTests.checkRouting(network.getNodes());
	}

	private void killPartOfNetwork(INetwork network) {
		
		IChordRemote[] node_array = network.getNodes().toArray(new IChordRemote[]{});

		int network_size = node_array.length;
		int number_to_kill = Math.min(1, (int)(PROPORTION_TO_KILL * network_size));
		
		Set<Integer> victim_indices = pickRandom(number_to_kill, network_size);
		
		for (int victim_index : victim_indices) {
			IChordRemote victim = node_array[victim_index];
			network.killNode(victim);
			
			// Wait for it to die.
			while (true) {
				try {
					victim.isAlive();
					Thread.sleep(DEATH_CHECK_INTERVAL);
				}
				catch (RemoteException e) {
					break;
				}
				catch (InterruptedException e) { }
			}
			
			network.getNodes().remove(victim);
		}
	}

	/**
	 * Returns a randomly selected subset of integers in a given range.
	 * @param number_to_select
	 * @param range
	 * @return a set of size number_to_select containing integers from 0 to range-1 inclusive
	 */
	private Set<Integer> pickRandom(int number_to_select, int range) {
		
		Set<Integer> set = new HashSet<Integer>();
		Random random = new Random(RANDOM_SEED);
		
		for (int i = 0; i < number_to_select; i++) {
			int choice = -1;
			while (choice == -1 || set.contains(choice))
				choice = random.nextInt(range);
			set.add(choice);
		}
		
		return set;
	}
}
