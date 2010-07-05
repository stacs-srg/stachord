/*******************************************************************************
 * StAChord Library
 * Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 * http://asa.cs.st-andrews.ac.uk/
 * 
 * This file is part of stachordRMI.
 * 
 * stachordRMI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * stachordRMI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with stachordRMI.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
 * Created on 08-Dec-2004
 *
 */
package uk.ac.standrews.cs.stachordRMI.util;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

/**
 * Implementation of segment/ring arithmetic.
 *
 * @author stuart, al, graham
 */
public class SegmentArithmetic {

    /******************** Segment Arithmetic Methods *********************/

    /**
     * Tests whether k is in the segment of the ring obtained by moving clockwise from (but not including) 'start'
     * until reaching (and including) 'end'. There are two cases, depending on whether the segment spans the
     * end/beginning of the key space:
     *
     * if ( start < end ) then ( start < k && k <= end )   // segment does not span end/beginning
     *                    else ( start < k || k <= end )   // segment does span end/beginning
     *
     * @param k the key to be tested
     * @param start key defining the start of the segment
     * @param end key defining the end of the segment
     * @return true if the test key is in the specified half open segment (including the end key)
     */
    public static boolean inHalfOpenSegment(IKey k, IKey start, IKey end) {
        if (start.compareTo(end) < 0)
			return start.compareTo(k) < 0 && k.compareTo(end) <= 0;
		else
			return start.compareTo(k) < 0 || k.compareTo(end) <= 0;
    }

    /**
     * Tests whether k is in the segment of the ring obtained by moving clockwise from (but not including) 'start'
     * until reaching (but not including) 'end'. There are two cases, depending on whether the segment spans the
     * end/beginning of the key space:
     *
     * if ( start < end ) then ( start < k && k < end )   // segment does not span end/beginning
     *                    else ( start < k || k < end )   // segment does span end/beginning
     *
     * @param k the key to be tested
     * @param start key defining the start of the segment
     * @param end key defining the end of the segment
     * @return true if the test key is in the specified open segment
     */
    public static boolean inOpenSegment(IKey k, IKey start, IKey end) {
        if (start.compareTo(end) < 0)
			return start.compareTo(k) < 0 && k.compareTo(end) < 0;
		else
			return start.compareTo(k) < 0 || k.compareTo(end) < 0;
    }

    /**
     * Tests whether k is in the segment of the ring obtained by moving clockwise from (and including) 'start'
     * until reaching (and including) 'end'. There are two cases, depending on whether the segment spans the
     * end/beginning of the key space:
     *
     * if ( start < end ) then ( start <= k && k <= end )   // segment does not span end/beginning
     *                    else ( start <= k || k <= end )   // segment does span end/beginning
     *
     * @param k the key to be tested
     * @param start key defining the start of the segment
     * @param end key defining the end of the segment
     * @return true if the test key is in the specified closed segment
     */
    public static boolean inClosedSegment(IKey k, IKey start, IKey end) {
        if (start.compareTo(end) < 0)
			return start.compareTo(k) <= 0 && k.compareTo(end) <= 0;
		else
			return start.compareTo(k) <= 0 || k.compareTo(end) <= 0;
    }
}
