package uk.ac.standrews.cs.stachord.recovery;

import java.io.IOException;
import org.junit.experimental.categories.Category;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.test.category.Ignore;

@Category(Ignore.class)
public class LocalMultipleProcessRecoveryTest extends ParameterizedRecoveryTest {

    public LocalMultipleProcessRecoveryTest(final int ring_size, final KeyDistribution key_distribution) throws IOException {

        super(ring_size, key_distribution);
    }

    @Override
    protected ChordNetwork createNetwork(final int ring_size, final KeyDistribution key_distribution) throws IOException {

        return new LocalMultipleProcessChordNetwork(ring_size, key_distribution);
    }
}
