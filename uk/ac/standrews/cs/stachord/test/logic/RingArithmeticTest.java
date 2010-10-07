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

package uk.ac.standrews.cs.stachord.test.logic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachord.impl.RingArithmetic;

/**
 * Tests ring arithmetic.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class RingArithmeticTest {

	/**
	 * Tests ring arithmetic.
	 */
	@Test
	public void segmentTest() {
		
		BigInteger i1 = Key.KEYSPACE_SIZE.divide(new BigInteger("4"));
		BigInteger i2 = i1.multiply(new BigInteger("2"));
		BigInteger i4 = i1.multiply(new BigInteger("3"));
		
		BigInteger i3 = i4.subtract(BigInteger.ONE);
		BigInteger i5 = i4.add(BigInteger.ONE);
		
		IKey k1 = new Key(i1);
		IKey k2 = new Key(i2);
		IKey k3 = new Key(i3);
		IKey k4 = new Key(i4);
		IKey k5 = new Key(i5);
		
		assertFalse(RingArithmetic.inHalfOpenSegment(k1, k2, k3));
		assertTrue( RingArithmetic.inHalfOpenSegment(k1, k3, k2));
		assertTrue( RingArithmetic.inHalfOpenSegment(k2, k1, k3));
		assertFalse(RingArithmetic.inHalfOpenSegment(k2, k3, k1));
		assertFalse(RingArithmetic.inHalfOpenSegment(k3, k1, k2));
		assertTrue( RingArithmetic.inHalfOpenSegment(k3, k2, k1));
		
		assertTrue( RingArithmetic.inHalfOpenSegment(k1, k2, k2));
		assertTrue( RingArithmetic.inHalfOpenSegment(k2, k1, k2));
		assertFalse(RingArithmetic.inHalfOpenSegment(k2, k2, k1));
		
		assertTrue( RingArithmetic.inHalfOpenSegment(k1, k1, k1));
		
		assertFalse(RingArithmetic.inHalfOpenSegment(k3, k4, k5));
		assertTrue( RingArithmetic.inHalfOpenSegment(k3, k5, k4));
		assertTrue( RingArithmetic.inHalfOpenSegment(k4, k3, k5));
		assertFalse(RingArithmetic.inHalfOpenSegment(k4, k5, k3));
		assertFalse(RingArithmetic.inHalfOpenSegment(k5, k3, k4));
		assertTrue( RingArithmetic.inHalfOpenSegment(k5, k4, k3));
	}
}
