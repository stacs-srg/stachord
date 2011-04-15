package uk.ac.standrews.cs.stachord.test.recovery;

import uk.ac.standrews.cs.nds.p2p.network.INetwork;
import uk.ac.standrews.cs.nds.p2p.network.KeyDistribution;

public class LocalRecoveryTestsSameProcessWithNetwork extends LocalRecoveryTestBase {

    //    private static final int[] RING_SIZES = {1, 2, 3, 4, 5, 10, 20};

    private static final int[] RING_SIZES = {2};

    @Override
    protected INetwork getTestNetwork(final int ring_size, final KeyDistribution network_type) throws Exception {

        return new LocalChordNetworkSingleProcess(ring_size, network_type);
    }

    @Override
    protected int[] getRingSizes() {

        return RING_SIZES;
    }
}
