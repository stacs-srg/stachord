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

package uk.ac.standrews.cs.stachordRMI.fingerTableFactories;

import uk.ac.standrews.cs.nds.p2p.interfaces.IDistanceCalculator;
import uk.ac.standrews.cs.stachordRMI.impl.DecimalGeometricSegments;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTable;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTableFactory;
import uk.ac.standrews.cs.stachordRMI.interfaces.ISegmentRangeCalculator;
import uk.ac.standrews.cs.stachordRMI.simulation.GlobalViewGeometricFingerTable;
import uk.ac.standrews.cs.stachordRMI.simulation.P2PSimulationProxy;

/**
 * @author stuart
 */
public class GlobalViewGeometricFingerTableFactory implements IFingerTableFactory {

	P2PSimulationProxy<IChordNode> psp;
	IDistanceCalculator dc;
	double decimalConstant;

	public GlobalViewGeometricFingerTableFactory(double decimalConstant, IDistanceCalculator dc, P2PSimulationProxy<IChordNode> psp){
		this.dc=dc;
		this.decimalConstant=decimalConstant;
		this.psp=psp;
	}

	public IFingerTable makeFingerTable(IChordNode localNode) {
		ISegmentRangeCalculator src = new DecimalGeometricSegments(localNode,decimalConstant);
		return new GlobalViewGeometricFingerTable(localNode,src,dc,psp);
	}

}
