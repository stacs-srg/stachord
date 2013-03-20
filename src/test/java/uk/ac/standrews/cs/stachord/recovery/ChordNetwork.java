package uk.ac.standrews.cs.stachord.recovery;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;

public class ChordNetwork extends ApplicationNetwork {

    private static final long serialVersionUID = -2865527843949516706L;
    protected final ChordManager default_manager;

    public ChordNetwork(final ChordManager default_manager) {

        super("Chord Application Network");
        this.default_manager = default_manager;
    }

    protected void configure(final int network_size, final KeyDistribution key_destribution, final Host host) {

        if (network_size < 0) { throw new IllegalArgumentException("network size must be greater than zero"); }
        final IKey[] node_keys = key_destribution.generateKeys(network_size);
        for (final IKey node_key : node_keys) {
            final ChordNodeDescriptor descriptor = new ChordNodeDescriptor(node_key, host, default_manager);
            add(descriptor);
        }
    }

}
