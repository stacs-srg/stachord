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

import java.net.InetAddress;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.eventModel.IEventGenerator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IDistanceCalculator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.InvalidSegmentNumberException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTable;
import uk.ac.standrews.cs.stachordRMI.interfaces.ISegmentRangeCalculator;
import uk.ac.standrews.cs.stachordRMI.util.SegmentArithmetic;

/**
 * @author stuart
 */
public class GeometricFingerTable extends AbstractFingerTable implements IFingerTable {

	private final int search_distance;
	private final boolean constrained;
	private IDistanceCalculator distance_calculator;

	public GeometricFingerTable(IChordNode node, ISegmentRangeCalculator segment_calculator, IDistanceCalculator distance_calculator, int search_distance, boolean constrained) {

		super(node, segment_calculator);

		this.search_distance = search_distance;
		this.constrained = constrained;
		this.distance_calculator = distance_calculator;
	}

	public GeometricFingerTable(IChordNode node, ISegmentRangeCalculator segment_calculator, IEventGenerator event_generator) {

		super(node, segment_calculator);

		search_distance = 0;
		constrained = true;
		setEventGenerator(event_generator);
	}

	@Override
	protected boolean addFinger(IChordRemote finger, int segment_number){

		try {
			if (search_distance == 0) return super.addFinger(finger, segment_number);

			if (!constrained)         return super.addFinger(closestNodeUnconstrained(finger, segment_number), segment_number);
			else                      return super.addFinger(closestNodeConstrained(finger, segment_number), segment_number);
		}
		catch (InvalidSegmentNumberException e) {
			Diagnostic.trace(DiagnosticLevel.FULL, "invalid segment number");
			return false;
		}
	}

	private IChordRemote closestNodeConstrained(IChordRemote startNode, int segmentNumber) throws InvalidSegmentNumberException {

		IChordRemote closest = startNode;
		IChordRemote right = startNode;

		KeyRange range = getSegmentCalculator().currentSegmentRange();

		IKey startKey = startNode.getKey();

		double distance = distance_calculator.distance(getNode().getAddress().getAddress(), startNode.getAddress().getAddress());

		for (int i = 0; i < search_distance; i++) {
			try {
				right = right.getSuccessor();
			}
			catch (Exception e) {
				//stop looking to the right
				Diagnostic.trace(DiagnosticLevel.RUN, "Call to getSuccessor() on 'right' node failed.");
				return closest;
			}

			if (right != null) {

				IKey rightKey = null;
				InetAddress rightIP = null;

				rightKey = right.getKey();
				try {
					rightIP = right.getAddress().getAddress();
				}
				catch (Exception e) {
					//stop looking to the right
					Diagnostic.trace(DiagnosticLevel.RUN, "Call to getKey() on 'right' node failed.");
					return closest;
				}

				if (rightKey != null && !getNode().getKey().equals(rightKey) && SegmentArithmetic.inClosedSegment(rightKey, startKey, range.getUpperBound())) {

					double rightDistance = distance_calculator.distance(getNode().getAddress().getAddress(), rightIP);

					if (rightDistance < distance) {
						distance = rightDistance;
						closest = right;
					}
				} else return closest;
			}
		}
		return closest;
	}

	private IChordRemote closestNodeUnconstrained(IChordRemote startNode, int segmentNumber) {

		IChordRemote closest = startNode;
		IChordRemote left = startNode;
		IChordRemote right = startNode;

		IChordRemote succ = null;
		try {
			succ = getNode().getSuccessor();
		} catch (RemoteException e1) {
			ErrorHandling.hardError( "error getting successor of node - should not happen local node" );
		}
		IKey succKey = succ.getKey();

		double distance = distance_calculator.distance(getNode().getAddress().getAddress(), startNode.getAddress().getAddress());

		for (int i = 0; i < search_distance; i++) {
			if (left != null) {

				try {
					left = left.getPredecessor();
				}
				catch (Exception e) {
					//stop looking to the left
					Diagnostic.trace(DiagnosticLevel.RUN, "error getting predecessor of 'left' node");
					left = null;
				}

				if (left != null) {

					IKey leftKey = null;
					InetAddress leftIP = null;

					leftKey = left.getKey();
					try {
						leftIP = left.getAddress().getAddress();
					}
					catch (Exception e) {
						//stop looking to the left
						Diagnostic.trace(DiagnosticLevel.RUN, "error getting representation of 'left' node");
						left = null;
					}

					if (leftKey != null && !succKey.equals(leftKey) && !getNode().getKey().equals(leftKey)) {

						double leftDistance = distance_calculator.distance(getNode().getAddress().getAddress(), leftIP);

						if (leftDistance < distance) {
							distance = leftDistance;
							closest = left;
						}
					}
				}
			}

			if (right != null) {
				try {
					right = right.getSuccessor();
				} catch (Exception e) {
					//stop looking to the right
					Diagnostic.trace(DiagnosticLevel.RUN, "error getting successor of 'right' node");
					right = null;
				}

				if (right != null) {

					IKey rightKey = right.getKey();
					InetAddress rightIP = right.getAddress().getAddress();

					if (rightKey != null && !succKey.equals(rightKey) && !getNode().getKey().equals(rightKey)) {

						double rightDistance = distance_calculator.distance(getNode().getAddress().getAddress(), rightIP);

						if (rightDistance < distance) {
							distance = rightDistance;
							closest = right;
						}
					}
				}
			}

			if (left.getKey().equals(right.getKey())) return closest;
		}
		return closest;
	}
}
