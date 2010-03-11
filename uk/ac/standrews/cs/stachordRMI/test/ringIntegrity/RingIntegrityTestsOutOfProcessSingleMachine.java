package uk.ac.standrews.cs.stachordRMI.test.ringIntegrity;

import org.junit.Before;

import uk.ac.standrews.cs.stachordRMI.test.factory.OutOfProcessSingleMachineFactory;

public class RingIntegrityTestsOutOfProcessSingleMachine extends RingIntegrityTests {
	
	@Before
	public void setUp() throws Exception {
		
		super.setUp();
		network_factory = new OutOfProcessSingleMachineFactory();
	}
}
