package uk.ac.standrews.cs.stachord.recovery;

import java.io.IOException;

import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

public class LocalChordNetworkSingleProcess extends ChordNetwork {

    private final LocalHost localHost;

    public LocalChordNetworkSingleProcess(final int network_size, final KeyDistribution key_distribution) throws IOException {

        super(new SingleProcessChordManager());
        localHost = new LocalHost();
        configure(network_size, key_distribution, localHost);
    }
}
