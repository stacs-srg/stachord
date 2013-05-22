package uk.ac.standrews.cs.stachord.recovery;

import java.io.IOException;

import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

public class LocalChordNetworkMultipleProcess extends ChordNetwork {

    private final LocalHost local_host;

    public LocalChordNetworkMultipleProcess(final int network_size, final KeyDistribution key_distribution) throws IOException {

        super(new MultipleProcessChordManager());
        local_host = new LocalHost();
        configure(network_size, key_distribution, local_host);
    }
}
