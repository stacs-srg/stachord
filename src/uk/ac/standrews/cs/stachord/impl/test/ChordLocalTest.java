/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
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
package uk.ac.standrews.cs.stachord.impl.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.math.BigInteger;
import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;

/**
 * Local Chord tests.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ChordLocalTest {

    private static final int PORT = 10000;
    private IKey key;
    private IChordNode chord_node;

    /**
     * Sets up test.
     * @throws Exception if the test cannot be set up.
     */
    @Before
    public void setup() throws Exception {

        key = new Key(new BigInteger("3"));
        chord_node = new ChordNodeFactory().createNode(new InetSocketAddress(NetworkUtil.getLocalIPv4Address(), PORT), key);
    }

    /**
     * Cleans up test.
     */
    @After
    public void teardown() {

        chord_node.shutDown();
    }

    /**
     * Tests whether the key can be retrieved successfully.
     */
    @Test
    public void getKey() {

        assertThat(chord_node.getKey(), is(equalTo(key)));
    }

    /**
     * Tests whether the successor of the node is itself.
     * @throws Exception if the test fails
     */
    @Test
    public void successorOneNodeRing() throws Exception {

        assertThat(chord_node.getSuccessor().getRemote().getKey(), is(equalTo(chord_node.getKey())));
    }

    /**
     * Tests whether the predecessor of the node is null.
     * @throws Exception if the test fails
     */
    @Test
    public void predecessorOneNodeRing() throws Exception {

        assertThat(chord_node.getPredecessor(), is(nullValue()));
    }
}
