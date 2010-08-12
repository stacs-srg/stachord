/*******************************************************************************
 * StAChord Library
 * Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 * http://asa.cs.st-andrews.ac.uk/
 * 
 * This file is part of stachordRMI.
 * 
 * stachordRMI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * stachordRMI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with stachordRMI.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package uk.ac.standrews.cs.stachordRMI.test.routing;

import java.io.IOException;
import java.rmi.NotBoundException;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.test.factory.INetwork;
import uk.ac.standrews.cs.stachordRMI.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachordRMI.test.factory.SingleMachineNetwork;
import uk.ac.standrews.cs.stachordRMI.test.util.TestLogic;

public abstract class RoutingTests {
	
	private static final int[] RING_SIZES = {1,2,3,4,6,10,20};

	@Before
	public void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
	}
	
	@Test
	public void routingBecomesCorrect() throws IOException, NotBoundException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println();
			Diagnostic.trace("testing routing for ring size: " + ring_size);
			
			routingBecomesCorrect(ring_size, KeyDistribution.RANDOM);
			routingBecomesCorrect(ring_size, KeyDistribution.EVEN);
			routingBecomesCorrect(ring_size, KeyDistribution.CLUSTERED);
		}
	}
	
	private void routingBecomesCorrect(int ring_size, KeyDistribution network_type) throws IOException, NotBoundException {

		INetwork network = new SingleMachineNetwork(ring_size, network_type);
		
		TestLogic.waitForStableRing(network.getNodes());
		TestLogic.waitForCorrectRouting(network.getNodes());
		
		network.killAllNodes();
	}
}
