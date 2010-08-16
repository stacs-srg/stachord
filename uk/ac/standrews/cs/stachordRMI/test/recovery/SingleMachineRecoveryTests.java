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
package uk.ac.standrews.cs.stachordRMI.test.recovery;

import java.io.IOException;
import java.rmi.NotBoundException;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachordRMI.test.factory.SingleMachineNetwork;

public class SingleMachineRecoveryTests {
	
	// TODO Make this work on Windows.
	
	private static final int[] RING_SIZES = {1,2,3,4,5,10,20};

	@Before
	public void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
	}
	
	@Test
	public void ringRecoversRandom() throws IOException, NotBoundException, InterruptedException {
			
		ringRecovers(KeyDistribution.RANDOM);
	}
	
	@Test
	public void ringRecoversEven() throws IOException, NotBoundException, InterruptedException {
		
		ringRecovers(KeyDistribution.EVEN);
	}

	@Test
	public void ringRecoversClustered() throws IOException, NotBoundException, InterruptedException {
		
		ringRecovers(KeyDistribution.CLUSTERED);
	}

	private void ringRecovers(KeyDistribution network_type) throws IOException, NotBoundException, InterruptedException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println("testing recovery for ring size: " + ring_size + ", network type: " + network_type);
			
			ringRecovers(ring_size, network_type);
		}
	}
	
	private void ringRecovers(int ring_size, KeyDistribution network_type) throws IOException, NotBoundException, InterruptedException {
		
		TestLogic.ringRecoversFromNodeFailure(new SingleMachineNetwork(ring_size, network_type));
	}
}
