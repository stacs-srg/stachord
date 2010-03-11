package uk.ac.standrews.cs.stachordRMI.test.ringIntegrity;

import org.junit.Before;

import uk.ac.standrews.cs.stachordRMI.test.factory.InProcessFactory;

public class RingIntegrityTestsInProcess extends RingIntegrityTests {
	
	@Before
	public void setUp() throws Exception {
		
		super.setUp();
		network_factory = new InProcessFactory();
	}
}
