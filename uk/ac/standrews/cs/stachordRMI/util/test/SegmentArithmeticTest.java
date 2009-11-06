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
 * Created on Dec 15, 2004 at 9:59:25 PM.
 */
package uk.ac.standrews.cs.stachordRMI.util.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.stachordRMI.util.SegmentArithmetic;

/**
 * Test class for SegmentArithmetic.
 *
 * @author graham
 */
public class SegmentArithmeticTest {

    private static Key k1 = new Key(BigInteger.ZERO);
    private static Key k2 = new Key(BigInteger.ONE);
    private static Key k3 = new Key(new BigInteger("3247823487234"));
    private static Key k4 = new Key(Key.KEYSPACE_SIZE.subtract(BigInteger.ONE));

    /**
     * Tests whether segment membership works as expected.
     */
    @Test
    public void inHalfOpenSegment() {

        assertTrue(SegmentArithmetic.inHalfOpenSegment(k2, k1, k3));
        assertTrue(SegmentArithmetic.inHalfOpenSegment(k3, k1, k4));
        assertTrue(SegmentArithmetic.inHalfOpenSegment(k3, k1, k3));

        assertFalse(SegmentArithmetic.inHalfOpenSegment(k2, k3, k1));
        assertFalse(SegmentArithmetic.inHalfOpenSegment(k3, k4, k1));
        assertFalse(SegmentArithmetic.inHalfOpenSegment(k3, k3, k1));

        assertFalse(SegmentArithmetic.inHalfOpenSegment(k1, k1, k2));
        assertTrue(SegmentArithmetic.inHalfOpenSegment(k2, k1, k2));
    }

    /**
     * Tests whether segment membership works as expected.
     */
    @Test
    public void inOpenSegment() {

        assertTrue(SegmentArithmetic.inOpenSegment(k2, k1, k3));
        assertTrue(SegmentArithmetic.inOpenSegment(k3, k1, k4));
        assertFalse(SegmentArithmetic.inOpenSegment(k3, k1, k3));

        assertFalse(SegmentArithmetic.inOpenSegment(k2, k3, k1));
        assertFalse(SegmentArithmetic.inOpenSegment(k3, k4, k1));
        assertFalse(SegmentArithmetic.inOpenSegment(k3, k3, k1));

        assertFalse(SegmentArithmetic.inOpenSegment(k1, k1, k2));
        assertFalse(SegmentArithmetic.inOpenSegment(k2, k1, k2));
    }
}
