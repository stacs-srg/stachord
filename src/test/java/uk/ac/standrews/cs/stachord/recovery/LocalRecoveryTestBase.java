/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland
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

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.ApplicationState;

/**
 * Tests Chord ring recovery after node failures, for rings of various sizes and for various patterns of key distribution.
 * Each Chord node is created in a separate process on the local machine.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public abstract class LocalRecoveryTestBase {

    private static final Duration CHECK_TIMEOUT = new Duration(10, TimeUnit.MINUTES); // Allow 10 minutes for each check operation.

    /**
     * Disables diagnostic.
     */
    @Before
    public void setUp() {

        Diagnostic.setLevel(DiagnosticLevel.NONE);
    }

    /**
     * Runs ring recovery tests with a {@link KeyDistribution#RANDOM} key distribution.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void ringRecoversRandom() throws Exception {

        ringRecovers(KeyDistribution.RANDOM);
    }

    private void ringRecovers(final KeyDistribution network_type) throws Exception {

        for (final int ring_size : getRingSizes()) {

            System.out.println("\n>>>>>>>>>>>>>>>> Testing recovery for ring size: " + ring_size + ", network type: " + network_type + "\n");
            ringRecovers(ring_size, network_type);
            System.out.println("\n>>>>>>>>>>>>>>>> Done");
        }
    }

    protected abstract int[] getRingSizes();

    // -------------------------------------------------------------------------------------------------------

    private void ringRecovers(final int ring_size, final KeyDistribution network_type) throws Exception {

        System.out.println("constructing ring... ");
        final Duration ring_creation_start = Duration.elapsed();
        final ChordNetwork network = getTestNetwork(ring_size, network_type);
        try {
            network.deployAll();
            network.awaitAnyOfStates(ApplicationState.RUNNING);
            RecoveryTestLogic.testRingRecoveryFromNodeFailure(network, CHECK_TIMEOUT, ring_creation_start);
        }
        finally {
            network.shutdown();
        }
    }

    protected abstract ChordNetwork getTestNetwork(final int ring_size, final KeyDistribution network_type) throws Exception;

    /**
     * Runs ring recovery tests with an {@link KeyDistribution#EVEN} key distribution.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void ringRecoversEven() throws Exception {

        ringRecovers(KeyDistribution.EVEN);
    }

    /**
     * Runs ring recovery tests with a {@link KeyDistribution#CLUSTERED} key distribution.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void ringRecoversClustered() throws Exception {

        ringRecovers(KeyDistribution.CLUSTERED);
    }
}
