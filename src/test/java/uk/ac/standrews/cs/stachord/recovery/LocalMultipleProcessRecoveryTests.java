package uk.ac.standrews.cs.stachord.recovery;

import java.io.IOException;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;

public class LocalMultipleProcessRecoveryTests extends ParameterizedRecoveryTest {

    protected LocalMultipleProcessRecoveryTests(final int ring_size, final KeyDistribution key_distribution) throws IOException {
        super(ring_size, key_distribution);
    }

    @Override
    protected ChordNetwork createNetwork(final int ring_size, final KeyDistribution key_distribution) throws IOException {
        return new LocalMultipleProcessChordNetwork(ring_size, key_distribution);
    }
}
