package uk.ac.standrews.cs.stachordRMI.test;


import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.util.RingTraversor;

public class RingIntegrityTest {

	private static final int DEFAULT_PORT = 54446;
	private static int port = DEFAULT_PORT;
	
	TestSetUp tsup;
	
	@Before
	public void setUp() throws Exception {
		Diagnostic.setLevel(DiagnosticLevel.FULL);
		int known_node_port = port;
		
		tsup = new TestSetUp();
		IChordNode first = tsup.startChordRing( "localhost", port++ );
		if(  first == null ) {
			Diagnostic.trace( "Failed to create first node" );
		}
		for( int i = 0; i < 10; i++ ) {
			tsup.joinChordRing("localhost", port++, "localhost", known_node_port );
		}
		tsup.stabiliseRing();
	}

	@After
	public void tearDown() throws Exception {
		
	}
	
	@Test
	public void checkFormation() {
		assertTrue( RingTraversor.traverseJChordRing( tsup.allNodes.first().getProxy().getRemote() ) );
	}

	@Test
	public void killNode() throws InterruptedException {
		assertTrue( RingTraversor.traverseJChordRing( tsup.allNodes.first().getProxy().getRemote()  ) );
		tsup.deleteNode( tsup.allNodes.last() ); // destroy a node;
		
		tsup.stabiliseRing();
		assertTrue( RingTraversor.traverseJChordRing( tsup.allNodes.first().getProxy().getRemote() ) );
	}
	
	@Test
	public void killMultipleNodes() throws InterruptedException {
		assertTrue( RingTraversor.traverseJChordRing( tsup.allNodes.first().getProxy().getRemote()  ) );
		
		do {
			Random rnd = new Random(7655759);
			
		tsup.deleteNode( tsup.allNodes.toArray(new IChordNode[0])[rnd.nextInt(tsup.allNodes.size()-1)]); // destroy a random node;
		
		tsup.stabiliseRing();
		assertTrue( RingTraversor.traverseJChordRing( tsup.allNodes.first().getProxy().getRemote() ) );
		
		} while (tsup.allNodes.size() > 2);
	}
	
}
