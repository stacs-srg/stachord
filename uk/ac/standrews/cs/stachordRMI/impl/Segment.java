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

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

/**
 * Representation of a ring segment.
 */
public class Segment {

	private final IChordRemoteReference finger;
	private final int segment_number;

	/**
	 * @param finger
	 * @param segment_number
	 */
	public Segment(IChordRemoteReference finger, int segment_number) {
		this.finger = finger;
		this.segment_number = segment_number;
	}

	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof Segment)) return false;

		return this.getKey().equals(((Segment) obj).getKey());
	}

	/**
	 * @return the finger reference
	 */
	public IChordRemoteReference getFinger() {
		return finger;
	}

	/**
	 * @return the segment number
	 */
	public int getSegmentNumber() {
		return segment_number;
	}

	public IKey getKey() {
		return finger.getKey();
	}
}
