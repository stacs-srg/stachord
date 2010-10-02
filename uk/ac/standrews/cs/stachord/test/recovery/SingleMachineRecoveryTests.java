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
package uk.ac.standrews.cs.stachord.test.recovery;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.remote_management.UnknownPlatformException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachord.test.factory.SingleMachineNetwork;

/**
 * Tests Chord ring recovery after node failures, for rings of various sizes and for various patterns of key distribution.
 * Each Chord node is created in a separate process on the local machine.
 * 
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public class SingleMachineRecoveryTests {
	
	private static final int CHECK_TIMEOUT = 600000;     // Allow 10 minutes for each check operation.
	// TODO Make this work on Windows.
	
	private static final int[] RING_SIZES = {1,2,3,4,5,10,20};

	@Before
	void setUp() throws Exception {
		
		Diagnostic.setLevel(DiagnosticLevel.NONE);		
	}
	
	/**
	 * Runs ring recovery tests with a {@link KeyDistribution#RANDOM} key distribution.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws UnknownPlatformException
	 */
	@Test
	public void ringRecoversRandom() throws IOException, NotBoundException, InterruptedException, TimeoutException, UnknownPlatformException {
			
		ringRecovers(KeyDistribution.RANDOM);
	}
	
	/**
	 * Runs ring recovery tests with an {@link KeyDistribution#EVEN} key distribution.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws UnknownPlatformException
	 */
	@Test
	public void ringRecoversEven() throws IOException, NotBoundException, InterruptedException, TimeoutException, UnknownPlatformException {
		
		ringRecovers(KeyDistribution.EVEN);
	}

	/**
	 * Runs ring recovery tests with a {@link KeyDistribution#CLUSTERED} key distribution.
	 * 
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws UnknownPlatformException
	 */
	@Test
	public void ringRecoversClustered() throws IOException, NotBoundException, InterruptedException, TimeoutException, UnknownPlatformException {
		
		ringRecovers(KeyDistribution.CLUSTERED);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void ringRecovers(KeyDistribution network_type) throws IOException, InterruptedException, TimeoutException, UnknownPlatformException {

		for (int ring_size : RING_SIZES) {
			
			System.out.println("\n>>>>>>>>>>>>>>>> Testing recovery for ring size: " + ring_size + ", network type: " + network_type + "\n");
			ringRecovers(ring_size, network_type);
			System.out.println("\n>>>>>>>>>>>>>>>> Done");
		}
	}
	
	private void ringRecovers(int ring_size, KeyDistribution network_type) throws IOException, InterruptedException, TimeoutException, UnknownPlatformException {
		
		System.out.println("constructing ring... ");
		SingleMachineNetwork network = new SingleMachineNetwork(ring_size, network_type);
		System.out.println("done");

		RecoveryTestLogic.testRingRecoveryFromNodeFailure(network, CHECK_TIMEOUT);
	}
}
