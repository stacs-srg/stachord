package uk.ac.standrews.cs.stachord.test.recovery;

import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.p2p.network.INetwork;

public class LocalRecoveryTestsSeparateProcesses extends LocalRecoveryTestBase {

    private static final int[] RING_SIZES = {1, 2, 3, 4, 5, 10, 20};

    @Override
    protected INetwork getTestNetwork(final int ring_size, final KeyDistribution network_type) throws Exception {

        return new LocalChordNetwork(ring_size, network_type);
    }

    @Override
    protected int[] getRingSizes() {

        return RING_SIZES;
    }
}
