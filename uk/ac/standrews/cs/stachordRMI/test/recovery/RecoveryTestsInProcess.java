package uk.ac.standrews.cs.stachordRMI.test.recovery;

import org.junit.Before;

import uk.ac.standrews.cs.stachordRMI.test.factory.InProcessFactory;

public class RecoveryTestsInProcess extends RecoveryTests {
	
	@Before
	public void setUp() throws Exception {
		
		super.setUp();
		network_factory = new InProcessFactory();
	} 
}
