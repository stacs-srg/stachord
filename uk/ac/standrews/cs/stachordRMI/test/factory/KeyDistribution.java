package uk.ac.standrews.cs.stachordRMI.test.factory;

public enum KeyDistribution {

	/**
	 * Nodes randomly distributed around the ring.
	 */
	RANDOM,
	
	/**
	 * Nodes evenly distributed around the ring.
	 */
	EVEN,
	
	/**
	 * Nodes clustered tightly in one region of the ring.
	 */
	CLUSTERED
}
