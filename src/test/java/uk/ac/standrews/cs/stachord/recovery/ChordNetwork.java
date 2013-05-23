package uk.ac.standrews.cs.stachord.recovery;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;

abstract class ChordNetwork extends ApplicationNetwork {

    protected final ChordManager default_manager;

    static final AttributeKey<IKey> PEER_KEY = new AttributeKey<IKey>();
    static final AttributeKey<Integer> PEER_PORT = new AttributeKey<Integer>();
    static final Integer DEFAULT_PEER_PORT = 0;

    ChordNetwork(final ChordManager default_manager) {

        super("Chord Application Network");
        this.default_manager = default_manager;
    }

    protected void configure(final int network_size, final KeyDistribution key_distribution, final Host host) {

        if (network_size < 0) { throw new IllegalArgumentException("network size must be greater than zero"); }
        final IKey[] node_keys = key_distribution.generateKeys(network_size);
        for (final IKey node_key : node_keys) {
            final ApplicationDescriptor descriptor = createApplicationDescriptor(host, node_key);
            add(descriptor);
        }
    }

    protected ApplicationDescriptor createApplicationDescriptor(final Host host, final IKey peer_key) {
        final ApplicationDescriptor descriptor = new ApplicationDescriptor(host, default_manager);
        descriptor.setAttribute(PEER_KEY, peer_key);
        descriptor.setAttribute(PEER_PORT, DEFAULT_PEER_PORT);
        return descriptor;
    }
}
