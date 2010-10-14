/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of stachord, an independent implementation of         *
 * the Chord protocol (http://pdos.csail.mit.edu/chord/).                  *
 *                                                                         *
 * stachord is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * stachord is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with stachord.  If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                         *
 ***************************************************************************/

package uk.ac.standrews.cs.stachord.impl;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

/**
 * Ring arithmetic calculations.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class RingArithmetic {

    /**
     * Tests whether k is in the segment of the ring obtained by moving clockwise from (but not including) 'start'
     * until reaching (and including) 'end'. There are two cases, depending on whether the segment spans the
     * end/beginning of the key space:
     *
     * <pre>
     * if ( start < end ) then ( start < k && k <= end )   // segment does not span end/beginning
     *                    else ( start < k || k <= end )   // segment does span end/beginning
     * </pre>
     *
     * @param k the key to be tested
     * @param start key defining the start of the segment
     * @param end key defining the end of the segment
     * @return true if the test key is in the specified half open segment (including the end key)
     */
    public static boolean inHalfOpenSegment(final IKey k, final IKey start, final IKey end) {

        final boolean start_less_than_end = start.compareTo(end) < 0;

        return start_less_than_end && start.compareTo(k) < 0 && k.compareTo(end) <= 0 || !start_less_than_end && (start.compareTo(k) < 0 || k.compareTo(end) <= 0);
    }
}
