/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland
 * http://beast.cs.st-andrews.ac.uk/                                 *
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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.p2p.keys.RingArithmetic;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Finger table implementation.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
class FingerTable {

    private final IChordNode node; // The node of which this is the finger table.
    private final IKey node_key; // The node's key.

    private final IChordRemoteReference[] fingers; // References to the fingers.
    private final IKey[] finger_targets; // Keys used to select the fingers.

    private final int number_of_fingers; // Size of the finger table.
    private int next_finger_index; // Index of the next finger to be fixed.

    // Used to derive finger table size. Nothing will break if the ring
    // exceeds this size, but the finger table may become more sparse than ideal.
    private static final int MAX_ASSUMED_RING_SIZE = 1000;

    // The ratio between successive finger targets.
    private static final BigInteger INTER_FINGER_RATIO = new BigInteger(String.valueOf(IChordNode.INTER_FINGER_RATIO));

    // -------------------------------------------------------------------------------------------------------

    public FingerTable(final IChordNode node) {

        this.node = node;
        node_key = node.getKey();

        number_of_fingers = log(MAX_ASSUMED_RING_SIZE, IChordNode.INTER_FINGER_RATIO);
        next_finger_index = number_of_fingers - 1;

        fingers = new IChordRemoteReference[number_of_fingers];
        finger_targets = new IKey[number_of_fingers];

        initializeFingerTargetKeys();
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Fixes the next finger in the finger table.
     * @return true if the finger was changed.
     */
    boolean fixNextFinger() {

        final boolean changed = fixFinger(next_finger_index);
        next_finger_index--;
        if (next_finger_index < 0) {
            next_finger_index = number_of_fingers - 1;
        }
        return changed;
    }

    /**
     * Returns the finger that extends the furthest round the ring from this node without passing the given key.
     *
     * @param k the target key
     * @return the closest preceding finger to the key
     * @throws NoPrecedingNodeException if no suitable finger is found
     * @throws RPCException if an error occurs in accessing a finger's key
     */
    IChordRemoteReference closestPrecedingNode(final IKey k) throws NoPrecedingNodeException, RPCException {

        for (int i = number_of_fingers - 1; i >= 0; i--) {

            final IChordRemoteReference finger = fingers[i];

            // Finger may be null if it hasn't been fixed for the first time, or if its failure has been detected.

            // Looking for finger that lies before k from position of this node.
            // Ignore fingers pointing to this node.

            if (finger != null) {
                final IKey finger_key = finger.getCachedKey();
                if (!finger_key.equals(node_key) && RingArithmetic.inRingOrder(node_key, finger_key, k)) { return finger; }
            }
        }

        throw new NoPrecedingNodeException();
    }

    /**
     * Notifies the finger table of a broken finger.
     *
     * @param broken_finger the finger that is suspected to have failed
     * @throws RPCException if an error occurs in accessing a finger's key
     */
    void fingerFailure(final IChordRemoteReference broken_finger) throws RPCException {

        for (int i = number_of_fingers - 1; i >= 0; i--) {

            IChordRemoteReference finger;
            synchronized (this) {
                finger = fingers[i] != null ? fingers[i] : null;
            }

            if (finger != null && finger.getCachedKey().equals(broken_finger.getCachedKey())) {
                fingers[i] = null;
            }
        }
    }

    /**
     * Returns the contents of the finger table as a list.
     * @return the contents of the finger table as a list
     */
    List<IChordRemoteReference> getFingers() {

        return new CopyOnWriteArrayList<IChordRemoteReference>(fingers);
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {

        final StringBuilder buffer = new StringBuilder();
        buffer.append("\n");

        for (int i = number_of_fingers - 1; i >= 0; i--) {

            buffer.append("finger: " + i);
            if (fingers[i] == null) {
                buffer.append(" null");
            }
            else {
                try {
                    buffer.append(" key: " + fingers[i].getCachedKey());
                }
                catch (final RPCException e) {
                    buffer.append(" key: inaccessible");
                }
                buffer.append(" address: " + fingers[i].getCachedAddress());
            }
            buffer.append("\n");
        }

        return buffer.toString();
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Constructs the finger target keys by successively dividing the offset from this node by the inter-finger ratio.
     */
    private void initializeFingerTargetKeys() {

        BigInteger finger_offset = Key.KEYSPACE_SIZE;

        final BigInteger local_key = node.getKey().keyValue();

        for (int i = number_of_fingers - 1; i >= 0; i--) {

            finger_offset = finger_offset.divide(INTER_FINGER_RATIO);
            finger_targets[i] = new Key(local_key.add(finger_offset));
        }
    }

    /**
     * Returns the truncated log of an integer to a given base.
     *
     * @param n an integer
     * @param base the required base
     * @return the log to the given base
     */
    private static int log(final int n, final int base) {

        return (int) (Math.log10(n) / Math.log10(base));
    }

    /**
     * Sets the correct finger for a given index in the finger table, by routing to the corresponding key.
     *
     * @param finger_index the index
     * @return true if a new finger was established
     */
    private boolean fixFinger(final int finger_index) {

        try {
            final IKey target_key = finger_targets[finger_index];
            final IChordRemoteReference new_finger = node.lookup(target_key);

            IChordRemoteReference old_finger;
            synchronized (this) {
                old_finger = fingers[finger_index] != null ? fingers[finger_index] : null;
            }

            fingers[finger_index] = new_finger;
            return old_finger == null || !old_finger.getCachedKey().equals(new_finger.getCachedKey());
        }
        catch (final RPCException e) {
            return false;
        }
    }
}
