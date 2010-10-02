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
