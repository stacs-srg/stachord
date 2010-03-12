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
 * Created on Dec 9, 2004 at 10:04:47 AM.
 */
package uk.ac.standrews.cs.stachordRMI.impl;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.standrews.cs.nds.p2p.interfaces.IDistanceCalculator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.InvalidSegmentNumberException;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoPrecedingNodeException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.util.SegmentArithmetic;

/**
 * Finger table implementation.
 */
public class FingerTable {

	private final IChordNode node;
	private final SegmentCalculator segment_calculator;

	private final SortedSet<Segment> segments;
	protected IDistanceCalculator distance_calculator;

	public FingerTable(IChordNode node, SegmentCalculator segment_calculator) {

		this.node = node;
		this.segment_calculator = segment_calculator;

		TreeSet<Segment> ts = new TreeSet<Segment>(new SegmentComparator(node));
		segments = Collections.synchronizedSortedSet(ts);
	}

	public int size() {
		synchronized (segments) {
			return segments.size();
		}
	}

	protected boolean addFinger(IChordRemoteReference finger, int segmentNumber) {

		boolean newFingerTableEntryForSegment = false;

		// Create a RingSegment to put in the finger table
		Segment nss = new Segment(finger, segmentNumber);

		// Default policy - remove any existing entry whose segment number is
		// equal to segment_calculator.getCurrentSegment()
		Segment entry;
		do {
			//old entry
			entry = getEntryBySegmentNumber(segmentNumber);

			if (entry != null) {

				if (!entry.getKey().equals(nss.getKey())) {
					newFingerTableEntryForSegment = true;
				}
				synchronized (segments) {
					segments.remove(entry);
				}
			}
		} while (entry != null);

		// The TreeSet will deal with duplicate entries so just add the object.
		// Note - NodesegmentStructComparator deems two entries to be the same
		// iff the node's keys are the same and the segment numbers are the
		// same.
		synchronized (segments) {
			segments.add(nss);
		}
		return newFingerTableEntryForSegment;
	}

	private boolean fixFinger(IKey target_key, int segmentNumber) {

		if (target_key != null) {
			try {
				IChordRemoteReference finger = (IChordRemoteReference)node.lookup(target_key);
				
				if (finger != null &&
						! finger.getKey().equals( node.getKey() ) &&
							! finger.getKey().equals( node.getSuccessor().getKey() ) ) {
					return addFinger(finger, segmentNumber);
				}
			}
			catch (RemoteException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "Remote Exception routing to: " + target_key + " lookup failure ?"); // Al
			}
		}

		return false;
	}

	/**
	 * Fixes the next finger in the finger table.
	 */
	public void fixNextFinger() {

		try {
			IKey target_key = segment_calculator.nextSegmentLowerBound();

			int segment = segment_calculator.getCurrentSegment();

			fixFinger(target_key, segment);
		}
		catch (InvalidSegmentNumberException e) {
			// do nothing
		}
	}

	public void fixAllFingers() {

		try {
			int numberOfSegments = segment_calculator.numberOfSegments();			

			for (int i = 0; i < numberOfSegments; i++) {
				IKey target_key = segment_calculator.nextSegmentLowerBound();
				fixFinger(target_key, segment_calculator.getCurrentSegment());
			}
		}
		catch (InvalidSegmentNumberException e) {
			Diagnostic.trace(DiagnosticLevel.FULL, "invalid segment number");
		}
	}

	private Segment getEntryBySegmentNumber(int segmentNumber) {
		synchronized (segments) {
			for (Segment segment : segments)
				if (segment.getSegmentNumber() == segmentNumber) return segment;
		}
		return null;
	}

	private Segment getEntryByKey(IKey k) {

		synchronized (segments) {
			for (Segment segment : segments)
				if (segment.getKey().equals(k)) return segment;
		}
		return null;
	}

	public synchronized IChordRemoteReference closestPrecedingNode(IKey k) throws NoPrecedingNodeException {

		// The TreeSet segments contains RingSegment objects
		// which have a natural order from largest to smallest key, as defined
		// by RingSegmentComparator. Thus the TreeSet's iterator runs from
		// largest to smallest key - which is nice.
		synchronized (segments) {
			for (Segment segment : segments) {
				IChordRemoteReference next = segment.getFinger();
				if (next != null && SegmentArithmetic.inOpenSegment(segment.getKey(), node.getKey(), k))
					return next;
			}
		}
		throw new NoPrecedingNodeException();
	}

	public void notifySuspectedFailure(IChordRemoteReference suggestedNode) {

		Segment entry = null;
		do {
			try {
				entry = getEntryByKey(suggestedNode.getKey());

				if (entry != null) {
					synchronized (segments) {
						segments.remove(entry);
					}
					Diagnostic.trace(DiagnosticLevel.RUN, "removing node "
									+ suggestedNode.getRemote().getAddress()
									+ " from finger table");
				}
			} catch (RemoteException e) {
				// can't get to remote node - can't help!
			}
		} while (entry != null);

	}

	@Override
	public String toString() {

		StringBuilder buffer = new StringBuilder();
		int i = 0;

		buffer.append("FingerTable: " );
		if( segments.size() == 0 ) {
			buffer.append("empty" );
			return buffer.toString();
		}
		synchronized (segments) {
			for (Segment segment : segments){
				try {
					IChordRemoteReference j = segment.getFinger();
					if (j != null) {
						buffer.append("\nNode At Position :\t" + i++ + "\nIP Address :\t"
								+ j.getRemote().getAddress().getAddress().getHostAddress() + "\nKey :\t" + j.getKey() + "\n");
					} else {
						buffer.append("\nNode At Position :\t" + i++ + "\nnull");
					}

				} catch (Exception e) {
					ErrorHandling.exceptionError(e, "error getting representation of finger");
				}
			}
		}
		return buffer.toString();
	}

	/**
	 * @return a string representing the contents of the finger table omitting duplicate keys
	 */
	public String toStringCompact() {

		String fingerString = "";
		String substr = "\nNode At Position :\t" + 0;

		synchronized (segments) {
			Iterator<Segment> i = segments.iterator();
			IChordRemoteReference finger;
			if (!i.hasNext())
				return "Finger table is empty";
			finger = i.next().getFinger();
			try {
				if (segments.size() > 0) {
					InetAddress currentIpAddress = null;
					InetAddress prevIpAddress = null;
					IKey currentKey = null;
					IKey prevKey = null;

					prevIpAddress = finger.getRemote().getAddress().getAddress();
					prevKey = finger.getKey();

					int entryCounter = 1;

					while (i.hasNext()) {
						IChordRemoteReference jcr = i.next().getFinger();
						if (jcr == null) {
							currentKey = null;
							currentIpAddress = null;
						} else {
							currentKey = jcr.getKey();
							currentIpAddress = jcr.getRemote().getAddress().getAddress();
						}
						if ( prevKey != currentKey &&	// this line is an optimisation? - possibly not
								 (currentKey == null && prevKey != null || currentKey != null && prevKey == null || !currentKey.equals(prevKey))) {
							substr += "\nIP Address :\t" + prevIpAddress.getHostAddress() + "\nKey :\t" + prevKey + "\n";
							fingerString += substr;
							substr = "\nNode At Position :\t" + entryCounter++;
							prevIpAddress = currentIpAddress;
							prevKey = currentKey;
						} else {
							substr += "," + entryCounter++;
						}
					}
					substr += "\nIP Address :\t" + prevIpAddress.getHostAddress() + "\nKey :\t" + prevKey + "\n";
					fingerString += substr;
				}
			} catch (Exception e) {
				ErrorHandling.exceptionError(e, "error getting representation of finger");
			}
		}
		return fingerString;
	}

	public ArrayList<IChordRemoteReference> getFingers() {

		ArrayList<IChordRemoteReference> fingers = new ArrayList<IChordRemoteReference>();
		synchronized (segments) {
			for (Segment segment : segments) {
				fingers.add(segment.getFinger());
			}
		}
		return fingers;
	}
}
