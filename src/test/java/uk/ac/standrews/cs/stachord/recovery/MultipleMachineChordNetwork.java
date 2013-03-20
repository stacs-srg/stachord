package uk.ac.standrews.cs.stachord.recovery;

import java.util.Set;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.host.Host;

public class MultipleMachineChordNetwork extends ChordNetwork {

    private static final long serialVersionUID = 8698842502879913421L;

    public MultipleMachineChordNetwork(final Set<Host> hosts, final KeyDistribution key_distribution) {

        super(new MultipleProcessChordManager());
        final IKey[] node_keys = key_distribution.generateKeys(hosts.size());
        int i = 0;
        for (final Host host : hosts) {
            final ChordNodeDescriptor descriptor = new ChordNodeDescriptor(node_keys[i], host, default_manager);
            add(descriptor);
            i++;
        }
    }
}
