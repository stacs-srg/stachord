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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.Observable;
import java.util.Observer;

import uk.ac.standrews.cs.nds.eventModel.Event;
import uk.ac.standrews.cs.nds.p2p.exceptions.SimulatedFailureException;
import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.InvalidSegmentNumberException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.ISegmentRangeCalculator;
import uk.ac.standrews.cs.stachordRMI.util.SegmentArithmetic;

/**
 * @author stuart
 */
public class DecimalGeometricSegments extends AbstractSegmentCalculator implements ISegmentRangeCalculator, Observer {

	private static final double DEFAULT_DECIMAL_CONSTANT = 2.0;

	private BigDecimal decimalConstant;
	private IKey succKey;

	public DecimalGeometricSegments(IChordNode localNode, double decimalConstant) {

		super(localNode);
		if(decimalConstant<=1.0) {
			this.decimalConstant=new BigDecimal(DEFAULT_DECIMAL_CONSTANT);
		} else {
			this.decimalConstant=new BigDecimal(decimalConstant);
		}
		last_segment_number=calculateLastSegmentNumber();
	}

	@Override
	protected int calculateLastSegmentNumber(){
		int max=-1;
		BigInteger distance;
		do {
			distance=calculateDistance(++max);
		} while(distance.bitLength() < Key.KEY_LENGTH);
		return max;
	}

	@Override
	protected BigInteger calculateDistance(int exponent){
		if(exponent>0){
			BigDecimal value=decimalConstant;
			for(int i=1;i<exponent;i++) {
				value=value.multiply(decimalConstant);
			}
			return value.toBigInteger();
		} else
			return BigInteger.ONE;
	}

	/**
	 * We are optimising the finger table such that the first entry is always
	 * further away (in ring distance) than this node's successor.
	 * @throws InvalidSegmentNumberException
	 */
	@Override
	protected int nextSegment() throws InvalidSegmentNumberException {

		int counter=0;

		do {
			current_segment++;
			counter++;
			if(counter>last_segment_number+1){
				//we've gone through all of the possible segments and
				//have failed to find a key that is further away in
				//keyspace than this node's successor. Give up.
				current_segment = NO_VIABLE_SEGMENTS;
				throw new InvalidSegmentNumberException();
			}
			else if(current_segment > last_segment_number) {
				current_segment = 0;
			}

		}
		while (getNode().getKey().ringDistanceTo(succKey).compareTo(getNode().getKey().ringDistanceTo(segmentLowerBound(current_segment))) > 0);

		return current_segment;
	}

	public void update(Observable arg0, Object arg1) {

		if (arg1 instanceof Event) {

			if (((Event) arg1).getType().equals("SuccessorStateEvent")) {
				newSuccessor();
			}
		}
	}

	private void newSuccessor() {

		try {
			succKey = node.getSuccessor().getKey();
		}
		catch ( SimulatedFailureException e ) {

			// Ignore - getSuccessor() call may throw exception if simulating failure; in real deployment the call should never fail,
			// since node object is local.
		}
		catch( RemoteException e ) {
			// should never happen - local node.
		}
	}

	public int calculateSegmentNumber(IKey k) throws InvalidSegmentNumberException {

		for (int i = last_segment_number; i >= 0; i--) {

			KeyRange sr = segmentRange(i);

			if (SegmentArithmetic.inClosedSegment(k,sr.getLowerBound(),sr.getUpperBound()))
				//all keys are in [a,a] - so make sure that the lowerBound and the upperBound
				//are not the same key - this occurs when the segment number is 0 and the constant is 2
				//since the range in this case is [k+2^0,k+(2^1)-1] or [k+1,k+1]
				if (!sr.getLowerBound().equals(sr.getUpperBound()))
					return i;
		}
		throw new InvalidSegmentNumberException();
	}

	public int numberOfSegments() {

		int segment_counter = -1;

		try {
			do {
				segment_counter++;
				if (segment_counter > last_segment_number) return 0;
			}
			while (getNode().getKey().ringDistanceTo(succKey).compareTo(getNode().getKey().ringDistanceTo(segmentLowerBound(segment_counter))) > 0);
		}
		catch (InvalidSegmentNumberException e) {
			Diagnostic.trace(DiagnosticLevel.FULL, "invalid segment number: " + segment_counter);
			return 0;
		}

		return last_segment_number - segment_counter + 1;
	}
}
