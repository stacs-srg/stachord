package uk.ac.standrews.cs.stachord.recovery;

import java.io.IOException;

import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.host.LocalHost;

public class LocalChordNetworkMultipleProcess extends ChordNetwork {

    private static final long serialVersionUID = -6230607115512428677L;
    private final LocalHost localHost;

    public LocalChordNetworkMultipleProcess(final int network_size, final KeyDistribution key_distribution) throws IOException {

        super(new MultipleProcessChordManager());
        localHost = new LocalHost();
        configure(network_size, key_distribution, localHost);
    }
}
