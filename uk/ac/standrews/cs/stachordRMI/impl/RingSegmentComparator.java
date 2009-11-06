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
 * Created on 11-Feb-2005
 */
package uk.ac.standrews.cs.stachordRMI.impl;

import java.math.BigInteger;
import java.util.Comparator;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;


/**
 * Comparator for ring segments, relative to a given node.
 */
public class RingSegmentComparator implements Comparator<RingSegment> {

	private final IChordNode local_node;

	/**
	 * @param local_node
	 */
	public RingSegmentComparator(IChordNode local_node){
		this.local_node = local_node;
	}

	public int compare(RingSegment segment1, RingSegment segment2) {

		if (segment1 == null) return -1;
		if (segment2 == null) return 1;

		if (segment1.equals(segment2)) return 0;

		IKey key1 = segment1.getFinger().getKey();
		IKey key2 = segment2.getFinger().getKey();

		IKey local_key = local_node.getKey();

		BigInteger distance_to_segment1 = local_key.ringDistanceTo(key1);
		BigInteger distance_to_segment2 = local_key.ringDistanceTo(key2);

		return distance_to_segment1.compareTo(distance_to_segment2) < 0 ? 1 : -1;
	}
}
