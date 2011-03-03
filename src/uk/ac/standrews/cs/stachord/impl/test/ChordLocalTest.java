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

public class ChordLocalTest {

    IKey key;
    IChordNode chord_node;

    @Before
    public void setup() throws Exception {

        key = new Key(new BigInteger("3"));
        chord_node = new ChordNodeFactory().createNode(new InetSocketAddress(NetworkUtil.getLocalIPv4Address(), 10000), key);
    }

    @After
    public void teardown() {

        chord_node.shutDown();
    }

    @Test
    public void getKey() throws Exception {

        assertThat(chord_node.getKey(), is(equalTo(key)));
    }

    @Test
    public void successorOneNodeRing() throws Exception {

        assertThat(chord_node.getSuccessor().getRemote().getKey(), is(equalTo(chord_node.getKey())));
    }

    @Test
    public void predecessorOneNodeRing() throws Exception {

        assertThat(chord_node.getPredecessor(), is(nullValue()));
    }
}
