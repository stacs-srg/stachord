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
/*
 * Created on 10-Feb-2005
 */
package uk.ac.standrews.cs.stachordRMI.simulation;

import uk.ac.standrews.cs.nds.p2p.interfaces.IDistanceCalculator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.simulation.interfaces.IP2PSimulation;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.impl.AbstractFingerTable;
import uk.ac.standrews.cs.stachordRMI.impl.KeyRange;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.InvalidSegmentNumberException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTable;
import uk.ac.standrews.cs.stachordRMI.interfaces.ISegmentRangeCalculator;
import uk.ac.standrews.cs.stachordRMI.util.SegmentArithmetic;

/**
 * @author stuart
 */
public class GlobalViewGeometricFingerTable extends AbstractFingerTable implements IFingerTable {

	private final IDistanceCalculator dc;
	private final P2PSimulationProxy<IChordNode> psp;

	public GlobalViewGeometricFingerTable(IChordNode localNode, ISegmentRangeCalculator src, IDistanceCalculator dc, P2PSimulationProxy<IChordNode> psp) {
		super(localNode,src);
		this.dc=dc;
		this.psp=psp;
	}

	@Override
	protected boolean addFinger(IChordRemote finger, int segmentNumber){

		try {
			IChordRemote cfinger = closestNode();
			return super.addFinger(cfinger, segmentNumber);
		}
		catch (InvalidSegmentNumberException e) {
			return super.addFinger(finger, segmentNumber);
		}
	}

	private IChordRemote closestNode() throws InvalidSegmentNumberException {

		KeyRange range = getSegmentCalculator().currentSegmentRange();
		IChordRemote closest = null;
		double distance = 0.0;
		IP2PSimulation<IChordNode> theSim = psp.getSim();
		IChordNode nodes[] = theSim.getNodes().toArray(new IChordNode[]{});
		int node_count = theSim.getNodeCount();

		//[k+2^1, k+2^(i+1))
		for (int i = 0; i < node_count; i++) {
			IKey k = null;
			try {
				k = nodes[i].getKey();
				if (getNode().getKey().compareTo(k) != 0 && getNode().getSuccessor().getKey().compareTo(k) != 0)
					if (SegmentArithmetic.inClosedSegment(k, range.getLowerBound(), range.getUpperBound()))
						if (closest == null) {
							closest = nodes[i];
							distance = dc.distance(getNode().getAddress().getAddress(), nodes[i].getAddress().getAddress());
						} else {
							double newDistance = dc.distance(getNode().getAddress().getAddress(), nodes[i].getAddress().getAddress());
							if (newDistance < distance) {
								closest = nodes[i];
								distance = newDistance;
							} else if (newDistance == distance)
								//remember the one which is farthest away
								// in key-space
								if (!SegmentArithmetic.inClosedSegment(nodes[i].getKey(), getNode().getKey(), closest.getKey())) {
									closest = nodes[i];
									distance = newDistance;
								}
						}
			} catch (Exception e) {
				ErrorHandling.exceptionError(e, "ChordNode call threw exception");
				e.printStackTrace();
			}
		}
		return closest;
	}


}
