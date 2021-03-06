package uk.ac.standrews.cs.stachord.recovery;

import java.util.Set;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;

public class MultipleHostChordNetwork extends ChordNetwork {

    public MultipleHostChordNetwork(final Set<Host> hosts, final KeyDistribution key_distribution) {

        super(new MultipleProcessChordManager());
        final IKey[] node_keys = key_distribution.generateKeys(hosts.size());
        int i = 0;
        for (final Host host : hosts) {
            final ApplicationDescriptor descriptor = createApplicationDescriptor(host, node_keys[i]);
            add(descriptor);
            i++;
        }
    }
}
