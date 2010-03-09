package uk.ac.standrews.cs.stachordRMI.test.tests;


import static org.junit.Assert.assertTrue;

import java.rmi.RMISecurityManager;
import java.util.Random;
import java.util.SortedSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.test.factory.InProcessFactory;
import uk.ac.standrews.cs.stachordRMI.test.factory.OutOfProcessSingleMachineFactory;
import uk.ac.standrews.cs.stachordRMI.util.RingStabilizer;
import uk.ac.standrews.cs.stachordRMI.util.RingTraversor;

public class RingIntegrityTest {
	
	SortedSet<IChordRemote> nodes;
	
	private static final String RMI_POLICY_FILENAME = "rmiPolicy";
	
	@Before
	public void setUp() throws Exception {
		Diagnostic.setLevel(DiagnosticLevel.FULL);
		
		// RMI Policy runes from Ben
		
		System.setProperty("java.security.policy", RMI_POLICY_FILENAME); 
		if (System.getSecurityManager() == null) {
			ErrorHandling.error( "Cannot find secrity manager" );
			System.setSecurityManager(new RMISecurityManager());
		}
		
		// nodes = new InProcessFactory().makeNetwork( 10 );
		nodes = new OutOfProcessSingleMachineFactory().makeNetwork( 10 );
	}

	@After
	public void tearDown() throws Exception {
		
	}
	
//	@Test
//	public void checkFormation() {
//		assertTrue( RingTraversor.traverseJChordRing( fac.allNodes.first().getProxy().getRemote() ) );
//	}
//
//	@Test
//	public void killNode() throws InterruptedException {
//		assertTrue( RingTraversor.traverseJChordRing( fac.allNodes.first().getProxy().getRemote()  ) );
//		fac.deleteNode( fac.allNodes.last() ); // destroy a node;
//		
//		fac.stabiliseRing();
//		assertTrue( RingTraversor.traverseJChordRing( fac.allNodes.first().getProxy().getRemote() ) );
//	}
//	
//	@Test
//	public void killMultipleNodes() throws InterruptedException {
//		assertTrue( RingTraversor.traverseJChordRing( fac.allNodes.first().getProxy().getRemote()  ) );
//		
//		do {
//			Random rnd = new Random(7655759);
//			
//		fac.deleteNode( fac.allNodes.toArray(new IChordNode[0])[rnd.nextInt(nodes.size()-1)]); // destroy a random node;
//		
//		fac.stabiliseRing();
//		assertTrue( RingTraversor.traverseJChordRing( fac.allNodes.first().getProxy().getRemote() ) );
//		
//		} while (fac.allNodes.size() > 2);
//	}
	
	
	@Test
	public void ringStabilises() {
		stabiliseRing();
	}
	
	/*
	 *	Ensure the ring is stable before continuing with any tests. 
	 */
	public void stabiliseRing() {

		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "STABILIZING RING::::::");
		RingStabilizer.waitForStableNetwork(nodes);
	}
	
}
