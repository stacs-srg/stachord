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
package uk.ac.standrews.cs.stachordRMI.impl;

import java.math.BigInteger;
import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.InvalidSegmentNumberException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.ISegmentRangeCalculator;


/**
 * Skeleton segment calculator implementation.
 */
public abstract class AbstractSegmentCalculator implements ISegmentRangeCalculator {

	protected static final int NO_VIABLE_SEGMENTS = -1;

	protected IChordNode node;

	protected int last_segment_number;
	protected int current_segment;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	public AbstractSegmentCalculator(IChordNode node) {

		this.node = node;
		current_segment = NO_VIABLE_SEGMENTS;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected abstract int calculateLastSegmentNumber();
	protected abstract BigInteger calculateDistance(int exponent);
	protected abstract int nextSegment() throws InvalidSegmentNumberException;

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	public IKey nextSegmentLowerBound() throws InvalidSegmentNumberException {

		return segmentLowerBound(nextSegment());
	}

	public KeyRange currentSegmentRange() throws InvalidSegmentNumberException {

		return segmentRange(current_segment);
	}

	public KeyRange segmentRange(int segment_number) throws InvalidSegmentNumberException {

		if (validSegmentNumber(segment_number))
			return new KeyRange(lowerBound(segment_number), upperBound(segment_number));
		else
			throw new InvalidSegmentNumberException();
	}

	public int getCurrentSegment() {

		return current_segment;
	}

	public IChordNode getNode() {
		return node;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected IKey segmentLowerBound(int segmentNumber) throws InvalidSegmentNumberException {

		return lowerBound(segmentNumber);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean validSegmentNumber(int segment_number) {

		return segment_number >= 0 && segment_number <= last_segment_number;
	}

	private IKey lowerBound(int segmentNumber) throws InvalidSegmentNumberException {

		if (validSegmentNumber(segmentNumber)) {

			BigInteger distance_round_ring = calculateDistance(segmentNumber);
			return new Key(node.getKey().keyValue().add(distance_round_ring));
		} else {
			throw new InvalidSegmentNumberException();
		}
	}

	private IKey upperBound(int segmentNumber) throws InvalidSegmentNumberException {

		if (validSegmentNumber(segmentNumber)) {

			BigInteger distance_round_ring = calculateDistance(segmentNumber+1);
			distance_round_ring=distance_round_ring.subtract(BigInteger.ONE);
			return new Key(node.getKey().keyValue().add(distance_round_ring));
		} else {
			throw new InvalidSegmentNumberException();
		}
	}
}
