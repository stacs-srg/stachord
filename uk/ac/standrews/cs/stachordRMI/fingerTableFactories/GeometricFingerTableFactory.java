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

import uk.ac.standrews.cs.nds.eventModel.IEventGenerator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IDistanceCalculator;
import uk.ac.standrews.cs.stachordRMI.impl.DecimalGeometricSegments;
import uk.ac.standrews.cs.stachordRMI.impl.GeometricFingerTable;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTable;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTableFactory;
import uk.ac.standrews.cs.stachordRMI.interfaces.ISegmentRangeCalculator;

/**
 * @author stuart
 */
public class GeometricFingerTableFactory implements IFingerTableFactory {

	int searchDistance;
	IDistanceCalculator dc;
	boolean constrained;
	double decimalConstant;
	IEventGenerator event_generator;

	public GeometricFingerTableFactory(double decimalConstant, int searchDistance, IDistanceCalculator dc, boolean constrained){
		this.searchDistance=searchDistance;
		this.dc=dc;
		this.constrained=constrained;
		this.decimalConstant=decimalConstant;
	}

	public GeometricFingerTableFactory(){
		this(2.0,0,null,true);
	}

	public GeometricFingerTableFactory(int searchDistance){
		this(searchDistance,0,null,true);
	}

	public GeometricFingerTableFactory(double searchDistance){
		this(searchDistance,0,null,true);
	}

	public GeometricFingerTableFactory(IEventGenerator event_generator) {
		this(2.0,0,null,true);
		this.event_generator=event_generator;

	}

	public IFingerTable makeFingerTable(IChordNode localNode) {
		ISegmentRangeCalculator src = new DecimalGeometricSegments(localNode,decimalConstant);
		return new GeometricFingerTable(localNode,src, event_generator);
	}

}
