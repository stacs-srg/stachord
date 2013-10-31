/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of stachord, an independent implementation of         *
 * the Chord protocol (http://pdos.csail.mit.edu/chord/).                  *
 *                                                                         *
 * stachord is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * stachord is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with stachord.  If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                         *
 ***************************************************************************/

package uk.ac.standrews.cs.stachord.recovery;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.ApplicationState;
import uk.ac.standrews.cs.shabdiz.util.Combinations;

/**
 * Tests Chord ring recovery after node failures, for rings of various sizes and for various patterns of key distribution.
 * Each Chord node is created in a separate process on the local machine.
 *
 * @author Masih Hajiarabderkani (mh638@st-andrews.ac.uk)
 */
@RunWith(Parameterized.class)
public abstract class ParameterizedRecoveryTest {

    protected static final Integer[] RING_SIZES = {10};
    protected static final KeyDistribution[] KEY_DISTRIBUTIONS = {KeyDistribution.RANDOM, KeyDistribution.CLUSTERED, KeyDistribution.EVEN};
    private static final Duration CHECK_TIMEOUT = new Duration(10, TimeUnit.MINUTES); // Allow 10 minutes for each check operation.
    private final ChordNetwork network;
    private final int ring_size;
    private final KeyDistribution key_distribution;
    @Rule
    public Timeout global_timeout = new Timeout(20 * 60 * 1000);

    public ParameterizedRecoveryTest(final int ring_size, final KeyDistribution key_distribution) throws IOException {

        this.ring_size = ring_size;
        this.key_distribution = key_distribution;
        network = createNetwork(ring_size, key_distribution);
    }

    @Parameterized.Parameters(name = "{index} -  ring size:{0}, key distribution: {1}")
    public static Collection<Object[]> getParameters() {

        return Combinations.generateArgumentCombinations(new Object[][]{RING_SIZES, KEY_DISTRIBUTIONS});
    }

    /**
     * Disables diagnostics, deploys the network and awaits {@link ApplicationState#RUNNING} state.
     *
     * @throws Exception if network deployment fails or an error occurs while waiting for the network to reach {@link ApplicationState#RUNNING} state
     */
    @Before
    public void setUp() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);
        printTestDetails();
        network.deployAll();
        network.awaitAnyOfStates(ApplicationState.RUNNING);
    }

    /**
     * Runs ring recovery tests.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void testRecovery() throws Exception {

        final Duration ring_creation_start = Duration.elapsed();
        RecoveryTestLogic.testRingRecoveryFromNodeFailure(network, CHECK_TIMEOUT, ring_creation_start);
    }

    /** Shuts down the network. */
    @After
    public void tearDown() {

        network.shutdown();
        System.out.println("\n>>>>>>>>>>>>>>>> Done");
    }

    protected abstract ChordNetwork createNetwork(final int ring_size, final KeyDistribution key_distribution) throws IOException;

    private void printTestDetails() {

        final StringBuilder builder = new StringBuilder();
        builder.append("\n>>>>>>>>>>>>>>>> Testing recovery for network type: ").append(network.getClass().getSimpleName());
        builder.append(" ring size: ").append(ring_size);
        builder.append(", key distribution: ").append(key_distribution);
        System.out.println(builder.toString());
        System.out.println("constructing ring... ");
    }
}
