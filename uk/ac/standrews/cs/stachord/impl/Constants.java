package uk.ac.standrews.cs.stachord.impl;

/**
 * Defines Chord implementation constants.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class Constants {

	/**
	 * The maximum length of a node's successor list.
	 */
	public static final int MAX_SUCCESSOR_LIST_SIZE = 5;
	
	/**
	 * The ratio between successive finger target keys in the finger table.
	 */
	public static final int INTER_FINGER_RATIO = 2;
	
	/**
	 * The default RMI registry port.
	 */
	public static final int DEFAULT_RMI_REGISTRY_PORT = 1099;
}
