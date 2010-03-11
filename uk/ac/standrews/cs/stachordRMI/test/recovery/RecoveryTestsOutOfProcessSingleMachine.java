package uk.ac.standrews.cs.stachordRMI.test.recovery;

import org.junit.Before;

import uk.ac.standrews.cs.stachordRMI.test.factory.OutOfProcessSingleMachineFactory;

public class RecoveryTestsOutOfProcessSingleMachine extends RecoveryTests {
	
	@Before
	public void setUp() throws Exception {
		
		super.setUp();
		network_factory = new OutOfProcessSingleMachineFactory();
	}
}
