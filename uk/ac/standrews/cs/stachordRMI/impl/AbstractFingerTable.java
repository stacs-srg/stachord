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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import uk.ac.standrews.cs.nds.eventModel.IEventGenerator;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.FormatNodeInfo;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.InvalidSegmentNumberException;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoPrecedingNodeException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachordRMI.interfaces.IFingerTable;
import uk.ac.standrews.cs.stachordRMI.interfaces.ISegmentRangeCalculator;
import uk.ac.standrews.cs.stachordRMI.util.SegmentArithmetic;

/**
 * Skeleton finger table implementation.
 */
public abstract class AbstractFingerTable implements IFingerTable {

	private final IChordNode node;
	private final ISegmentRangeCalculator segment_calculator;
	private IEventGenerator event_generator;

	private final SortedSet<RingSegment> segments;

	public AbstractFingerTable(IChordNode node, ISegmentRangeCalculator segment_calculator) {

		this.node = node;
		this.segment_calculator = segment_calculator;

		TreeSet<RingSegment> ts = new TreeSet<RingSegment>(new RingSegmentComparator(node));
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
		RingSegment nss = new RingSegment(finger, segmentNumber);

		// Default policy - remove any existing entry whose segment number is
		// equal to segment_calculator.getCurrentSegment()
		RingSegment entry;
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

		Diagnostic.trace(DiagnosticLevel.FULL, "\tfixing segment:" + segmentNumber );
		Diagnostic.trace(DiagnosticLevel.FULL, "\tsegment key:" + target_key );
		if (target_key != null) {
			try {
				IChordRemoteReference finger = (IChordRemoteReference)node.lookup(target_key);
				Diagnostic.trace(DiagnosticLevel.FULL, "\tfound node with key:" + finger.getKey() );
				
				// node is of type IChordNode
				if (finger != null &&
						! finger.getKey().equals( node.getKey() ) &&
							! finger.getKey().equals( node.getSuccessor().getKey() ) ) { // AL changed
						Diagnostic.trace(DiagnosticLevel.FULL, "adding key:" + finger.getKey() );
						return addFinger(finger, segmentNumber);
				}
			}
			catch (RemoteException e) {
				Diagnostic.trace(DiagnosticLevel.FULL, "Remote Exeption routing to: " + target_key + " lookup failure ?"); // Al
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

			boolean fingerTableChange = fixFinger(target_key, segment);

		}
		catch (InvalidSegmentNumberException e) {
			Diagnostic.trace(DiagnosticLevel.FULL, "invalid segment number");
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

	private RingSegment getEntryBySegmentNumber(int segmentNumber) {
		synchronized (segments) {
			for (RingSegment segment : segments)
				if (segment.getSegmentNumber() == segmentNumber) return segment;
		}
		return null;
	}

	private RingSegment getEntryByKey(IKey k) {

		synchronized (segments) {
			for (RingSegment segment : segments)
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
			for (RingSegment segment : segments) {
				IChordRemoteReference next = segment.getFinger();
				if (next != null && SegmentArithmetic.inOpenSegment(segment.getKey(), node.getKey(), k))
					return next;
			}
		}
		throw new NoPrecedingNodeException();
	}

	/**
	 * The finger table implementation cannot contain this node or this node's successor.
	 *
	 * @param suggestedNode suggests a ChordNode that might be added to the finger table
	 */
	public void notifyExistence(IChordRemoteReference suggestedNode) {

			if (suggestedNode != null && !suggestedNode.equals(node) && !suggestedNode.equals(node.getSuccessor())) {

				try {
					IKey key = suggestedNode.getKey();
					int segmentNumber = segment_calculator.calculateSegmentNumber(key);
					addFinger(suggestedNode, segmentNumber);
				}
				catch (InvalidSegmentNumberException e) {
					// Ignore suggestions for nodes which lie outwith the set of defined segments
				}
			}
	}

	public void notifySuspectedFailure(IChordRemoteReference suggestedNode) {

		RingSegment entry = null;
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
			for (RingSegment segment : segments){
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
			Iterator<RingSegment> i = segments.iterator();
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

	public void setEventGenerator(IEventGenerator event_generator) {
		this.event_generator = event_generator;
	}

	public ArrayList<IChordRemoteReference> getFingers() {

		ArrayList<IChordRemoteReference> fingers = new ArrayList<IChordRemoteReference>();
		synchronized (segments) {
			for (RingSegment segment : segments) {
				fingers.add(segment.getFinger());
			}
		}
		return fingers;
	}

	public ISegmentRangeCalculator getSegmentCalculator() {
		return segment_calculator;
	}

	public IChordNode getNode() {
		return node;
	}
}
