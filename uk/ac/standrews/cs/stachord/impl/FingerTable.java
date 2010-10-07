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

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Finger table implementation.
 * 
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
class FingerTable {

	private final IChordNode node;                       // The node of which this is the finger table.
	private final IKey node_key;                         // The node's key.

	private IChordRemoteReference[] fingers;             // References to the fingers.
	private IKey[] finger_targets;                       // Keys used to select the fingers.
	
	private final int number_of_fingers;                 // Size of the finger table.
	private int next_finger_index;                       // Index of the next finger to be fixed.
	
	// Used to derive finger table size. Nothing will break if the ring
	// exceeds this size, but the finger table may become more sparse than ideal.
	private static final int MAX_ASSUMED_RING_SIZE = 1000;
	
	// The ratio between successive finger targets.
	private static final BigInteger INTER_FINGER_RATIO = new BigInteger(String.valueOf(Constants.INTER_FINGER_RATIO));

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	public FingerTable(IChordNode node) {

		this.node = node;
		node_key = node.getKey();
		
		number_of_fingers = log(MAX_ASSUMED_RING_SIZE, Constants.INTER_FINGER_RATIO);
		next_finger_index = number_of_fingers - 1;
		
		fingers = new IChordRemoteReference[number_of_fingers];
		finger_targets = new IKey[number_of_fingers];
		
		initFingerTargets();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Fixes the next finger in the finger table.
	 * @return true if the finger was changed.
	 */
	public boolean fixNextFinger() {

		boolean changed = fixFinger(next_finger_index);
		next_finger_index--;
		if (next_finger_index < 0) next_finger_index = number_of_fingers - 1;
		return changed;
	}

	/**
	 * Returns the finger that extends the furthest round the ring from this node without passing the given key.
	 * 
	 * @param key the target key
	 * @return the closest preceding finger to the key
	 * @throws NoPrecedingNodeException if no suitable finger is found
	 */
	public synchronized IChordRemoteReference closestPrecedingNode(IKey key) throws NoPrecedingNodeException {

		for (int i = number_of_fingers - 1; i >= 0; i--) {
			
			IChordRemoteReference finger = fingers[i];
			
			// Finger may be null if it hasn't been fixed for the first time,
			// or if its failure has been detected.
			
			// Looking for finger that lies before k from position of this node.
			// Ignore fingers pointing to this node.
			if (finger != null && !node_key.equals(finger.getKey()) && node_key.firstCloserInRingThanSecond(finger.getKey(), key)) {
				return finger;
			}
		}

		throw new NoPrecedingNodeException();
	}

	/**
	 * Notifies the finger table of a broken finger.
	 * @param broken_finger the finger that has failed
	 */
	public void fingerFailure(IChordRemoteReference broken_finger) {

		for (int i = number_of_fingers - 1; i >= 0; i--) {
			
			if (fingers[i] != null && fingers[i].getKey().equals(broken_finger.getKey())) {
				fingers[i] = null;
			}
		}
	}

	/**
	 * Returns the contents of the finger table as a list.
	 * @return the contents of the finger table as a list
	 */
	public List<IChordRemoteReference> getFingers() {

		ArrayList<IChordRemoteReference> finger_list = new ArrayList<IChordRemoteReference>();
		for (int i = 0; i < number_of_fingers; i++) finger_list.add(fingers[i]);
		return finger_list;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public String toString() {

		StringBuilder buffer = new StringBuilder();
		buffer.append("\n");
		
		for (int i = number_of_fingers - 1; i >= 0; i--) {
			
			buffer.append("finger: " + i);
			if (fingers[i] == null) buffer.append(" null");
			else {
				buffer.append(" key: " +     fingers[i].getKey() );
				buffer.append(" address: " + fingers[i].getAddress());
			}
			buffer.append("\n");
		}
		
		return buffer.toString();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructs the finger target keys by successively dividing the offset from this node by the inter-finger ratio.
	 */
	private void initFingerTargets() {
		
		BigInteger finger_offset = Key.KEYSPACE_SIZE;
		
		BigInteger local_key = node.getKey().keyValue();
		
		for (int i = number_of_fingers - 1; i >= 0; i--) {
			
			finger_offset = finger_offset.divide(INTER_FINGER_RATIO);
			finger_targets[i] = new Key(local_key.add(finger_offset));
		}
	}

	/**
	 * Returns the truncated log of an integer to a given base.
	 * @param n an integer
	 * @param base the required base
	 * @return the log to the given base
	 */
	private static int log(int n, int base) {

		return (int)(Math.log10(n)/Math.log10(base));
	}

	/**
	 * Sets the correct finger for a given index in the finger table, by routing to the corresponding key.
	 * 
	 * @param finger_index the index 
	 * @return true if a new finger was established
	 */
	private boolean fixFinger(int finger_index) {

		try {
			IKey target_key = finger_targets[finger_index];
			IChordRemoteReference finger = node.lookup(target_key);
			
			boolean changed = fingers[finger_index] == null || !fingers[finger_index].getKey().equals(finger.getKey());
			fingers[finger_index] = finger;
			return changed;
		}
		catch (RemoteException e) {
			return false;
		}
	}
}
