/*
 *  StAChord Library
 *  Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 *  http://asa.cs.st-andrews.ac.uk/
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.stachordRMI.testharness.impl;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.IChordNetwork;

public abstract class NetworkTestParent {

	protected static IChordNetwork<?> currentNetwork=null;

	protected static final int TIMEOUT = 300000;

	@Before
	public void setup() throws Exception{
		if(currentNetwork!=null){
			killServers();
			ErrorHandling.error("The network from the previous test was not destroyed properly");
		}
	}

	@After
	public void killServers() throws Exception{
		killRemainingServers();
	}

	// Needed to clear up if final test throws an exception. 'killServers' will not be called under those
	// circumstances.
	@AfterClass
	public static void killRemainingServers() throws Exception{
		if(currentNetwork!=null) {
			currentNetwork.killNetwork();
		}
		currentNetwork=null;
	}

	/**
	 *
	 * @throws Exception
	 */
	protected void check() throws Exception {
		if(currentNetwork != null) {
			throw new Exception("Cannot create a new network since the previously created network was not destroyed properly");
		}
	}

	public void resetNetworkFactory() throws Exception{
		killRemainingServers();
	}
}
