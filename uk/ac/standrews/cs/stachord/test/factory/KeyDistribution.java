package uk.ac.standrews.cs.stachord.test.factory;

/**
 * Options for distribution of a set of keys in key space.
 *
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
public enum KeyDistribution {

	/**
	 * Keys randomly distributed around the ring.
	 */
	RANDOM,
	
	/**
	 * Keys evenly distributed around the ring.
	 */
	EVEN,
	
	/**
	 * Keys clustered tightly in one region of the ring.
	 */
	CLUSTERED
}
