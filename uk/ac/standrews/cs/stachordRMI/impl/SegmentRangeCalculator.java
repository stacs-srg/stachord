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

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

/**
 * @author stuart
 */
public interface SegmentRangeCalculator {
	public IKey nextSegmentLowerBound();
	public IKey currentSegmentLowerBound();
	public KeyRange nextSegmentRange();
	public KeyRange currentSegmentRange();
	public int getCurrentSegment();
	public int calculateSegmentNumber(IKey k);
	public int numberOfSegments();
}
