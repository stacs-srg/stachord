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
package uk.ac.standrews.cs.stachordRMI.test;

import org.junit.Test;

import uk.ac.standrews.cs.stachordRMI.testharness.impl.SimulatedChordTest;
import uk.ac.standrews.cs.stachordRMI.testharness.logic.Routing;

public class RoutingSimulationTests extends SimulatedChordTest{
	
	@Test
	public void ringRouting() throws Exception{
		Routing.ringRouting(this);
	}
	
	@Test
	public void ringRoutingRandomised() throws Exception{
		Routing.ringRoutingRandomised(this);
	}
}
