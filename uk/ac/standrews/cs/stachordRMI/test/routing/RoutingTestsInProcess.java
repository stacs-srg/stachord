package uk.ac.standrews.cs.stachordRMI.test.routing;

import org.junit.Before;

import uk.ac.standrews.cs.stachordRMI.test.factory.InProcessFactory;

public class RoutingTestsInProcess extends RoutingTests {
	
	@Before
	public void setUp() throws Exception {
		
		super.setUp();
		network_factory = new InProcessFactory();
	}
}
