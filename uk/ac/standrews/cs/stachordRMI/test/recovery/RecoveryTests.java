package uk.ac.standrews.cs.stachordRMI.test.recovery;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.test.factory.AbstractNetworkFactory;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetworkFactory;
import uk.ac.standrews.cs.stachordRMI.test.util.TestLogic;

public abstract class RecoveryTests {
	
	protected INetworkFactory network_factory;

	private static final int[] RING_SIZES = {2,3,4};

	private static final double PROPORTION_TO_KILL = 0.2;

	private static final long DEATH_CHECK_INTERVAL = 2000;
	
	private static final int RANDOM_SEED = 32423545;
	
	@Before
	public void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
	}
	
	@Test
	public void ringRecovers() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println("testing recovery for ring size: " + ring_size);
			
//			ringRecovers(ring_size, AbstractNetworkFactory.RANDOM);
			ringRecovers(ring_size, AbstractNetworkFactory.EVEN);
			ringRecovers(ring_size, AbstractNetworkFactory.CLUSTERED);
		}
	}
	
	private void ringRecovers(int ring_size, String network_type) throws IOException, NotBoundException {

		INetwork network = network_factory.makeNetwork(ring_size, network_type);
		TestLogic.waitForStableRing(network.getNodes());
		
		ringRecovers(network);
		network.killAllNodes();
	}
	
	private void ringRecovers(INetwork network) throws IOException {
		
		// Routing should still eventually work even in the absence of finger table maintenance.
		enableFingerTableMaintenance(network, false);
		
		killPartOfNetwork(network);
		
		TestLogic.waitForCorrectRouting(network.getNodes());

		// Turn on maintenance again.
		enableFingerTableMaintenance(network, true);
		
		TestLogic.waitForStableRing(network.getNodes());

		TestLogic.waitForCompleteFingerTables(network.getNodes());

		TestLogic.waitForCompleteSuccessorLists(network.getNodes());
		
		TestLogic.waitForCorrectRouting(network.getNodes());
	}

	private void enableFingerTableMaintenance(INetwork network, boolean enabled) throws RemoteException {
		
		for (IChordRemoteReference node : network.getNodes()) node.getRemote().enableFingerTableMaintenance(enabled);
	}

	private void killPartOfNetwork(INetwork network) {
		
		IChordRemoteReference[] node_array = network.getNodes().toArray(new IChordRemoteReference[]{});

		int network_size = node_array.length;
		int number_to_kill = (int) Math.max(1, (int)(PROPORTION_TO_KILL * (double)network_size));
		
		Set<Integer> victim_indices = pickRandom(number_to_kill, network_size);
		
		for (int victim_index : victim_indices) {
			
			IChordRemoteReference victim = node_array[victim_index];			
			network.killNode(victim);
			
			// Wait for it to die.
			while (true) {
				try {
					victim.getRemote().isAlive();
					Thread.sleep(DEATH_CHECK_INTERVAL);
				}
				catch (RemoteException e) {
					break;
				}
				catch (InterruptedException e) { }
			}
		}
		
		assertEquals(network.getNodes().size(), network_size - number_to_kill);
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
