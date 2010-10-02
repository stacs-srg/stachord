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
	public static boolean inHalfOpenSegment(IKey k, IKey start, IKey end) {
		
	    boolean start_less_than_end = start.compareTo(end) < 0;
	    
		return ( start_less_than_end && (start.compareTo(k) < 0 && k.compareTo(end) <= 0)) ||
		       (!start_less_than_end && (start.compareTo(k) < 0 || k.compareTo(end) <= 0));
	}
}
