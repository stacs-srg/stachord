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
 * Created on Dec 9, 2004 at 10:04:47 AM.
 */
package uk.ac.standrews.cs.stachordRMI.impl;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.impl.exceptions.NoPrecedingNodeException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

/**
 * Finger table implementation.
 */
public class FingerTable {

	private final IChordNode node;

	private IChordRemoteReference[] fingers;
	private IKey[] finger_targets;
	
	private final int number_of_fingers;
	private int next_finger_index;
	
	// Used to derive finger table size. Nothing will break if the ring
	// exceeds this size, but the finger table may become more sparse than ideal.
	private static final int MAX_ASSUMED_RING_SIZE = 1000;
	
	// The ratio between successive fingers.
	private static final BigInteger INTER_FINGER_RATIO = new BigInteger("2");

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	public FingerTable(IChordNode node) {

		this.node = node;
		
		number_of_fingers = log2(MAX_ASSUMED_RING_SIZE);
		next_finger_index = number_of_fingers - 1;
		
		fingers = new IChordRemoteReference[number_of_fingers];
		finger_targets = new IKey[number_of_fingers];
		
		initFingerTargets();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Fixes the next finger in the finger table.
	 */
	public boolean fixNextFinger() {

		boolean changed = fixFinger(next_finger_index);
		next_finger_index--;
		if (next_finger_index < 0) next_finger_index = number_of_fingers - 1;
		return changed;
	}

	public synchronized IChordRemoteReference closestPrecedingNode(IKey k) throws NoPrecedingNodeException {

		for (int i = number_of_fingers - 1; i >= 0; i--) {
			
			IChordRemoteReference finger = fingers[i];
			
			// Finger may be null if it hasn't been fixed for the first time,
			// or if its failure has been detected.
			
			// Looking for finger that lies before k from position of this node.
			// Ignore fingers pointing to this node.
			if (finger != null && !node.getKey().equals(finger.getKey()) && node.getKey().firstCloserInRingThanSecond(finger.getKey(), k)) {
				return finger;
			}
		}

		throw new NoPrecedingNodeException();
	}

	public void fingerFailure(IChordRemoteReference broken_finger) {

		for (int i = number_of_fingers - 1; i >= 0; i--) {
			
			if (fingers[i] != null && fingers[i].getKey().equals(broken_finger.getKey())) {
				fingers[i] = null;
			}
		}
	}
	
	@Override
	public String toString() {

		StringBuilder buffer = new StringBuilder();
		buffer.append("\n");
		
		for (int i = number_of_fingers - 1; i >= 0; i--) {
			
			buffer.append("finger: " + i + " key: ");
			buffer.append(fingers[i] != null ? fingers[i].getKey() : "null");
			buffer.append(" address: " + fingers[i]);
			buffer.append("\n");
		}
		
		return buffer.toString();
	}

	public ArrayList<IChordRemoteReference> getFingers() {

		ArrayList<IChordRemoteReference> finger_list = new ArrayList<IChordRemoteReference>();
		for (int i = 0; i < number_of_fingers; i++) finger_list.add(fingers[i]);
		return finger_list;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void initFingerTargets() {
		
		BigInteger finger_offset = Key.KEYSPACE_SIZE;
		
		for (int i = number_of_fingers - 1; i >= 0; i--) {
			
			finger_offset = finger_offset.divide(INTER_FINGER_RATIO);
			finger_targets[i] = new Key(node.getKey().keyValue().add(finger_offset));
		}
	}

	private static int log2(int n) {

		return (int)(Math.log10(n)/Math.log10(2));
	}

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
