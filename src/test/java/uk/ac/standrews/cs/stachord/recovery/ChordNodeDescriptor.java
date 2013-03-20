package uk.ac.standrews.cs.stachord.recovery;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;

public class ChordNodeDescriptor extends ApplicationDescriptor {

    private final IKey node_key;
    private int node_port;

    public ChordNodeDescriptor(final IKey node_key, final Host host, final ChordManager manager) {

        super(host, manager);
        this.node_key = node_key;
    }

    public IKey getNodeKey() {

        return node_key;
    }

    public int getNodePort() {

        return node_port;
    }

    public void setNodePort(final int nodePort) {

        this.node_port = nodePort;
    }
}
