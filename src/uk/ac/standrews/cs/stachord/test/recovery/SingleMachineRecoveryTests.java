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

package uk.ac.standrews.cs.stachord.test.recovery;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.ProcessManager;
import uk.ac.standrews.cs.nds.madface.UnknownPlatformException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.servers.StartNodeInNewRing;
import uk.ac.standrews.cs.stachord.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachord.test.factory.SingleHostNetwork;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Tests Chord ring recovery after node failures, for rings of various sizes and for various patterns of key distribution.
 * Each Chord node is created in a separate process on the local machine.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class SingleMachineRecoveryTests {

    private static final int CHECK_TIMEOUT = 600000; // Allow 10 minutes for each check operation.
    // TODO Make this work on Windows.

    private static final int[] RING_SIZES = {1, 2, 3, 4, 5, 10, 20};

    /**
     * Disables diagnostic output and kills existing instances.
     * @throws IOException if existing instances cannot be killed
     * @throws TimeoutException shouldn't occur locally
     * @throws SSH2Exception shouldn't occur locally
     */
    @Before
    public void setUp() throws IOException, SSH2Exception, TimeoutException {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        // Kill any lingering Chord node processes.
        new ProcessManager().killMatchingProcesses(StartNodeInNewRing.class.getSimpleName());
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void ringRecovers(final KeyDistribution network_type) throws IOException, InterruptedException, TimeoutException, UnknownPlatformException, RPCException {

        for (final int ring_size : RING_SIZES) {

            System.out.println("\n>>>>>>>>>>>>>>>> Testing recovery for ring size: " + ring_size + ", network type: " + network_type + "\n");
            ringRecovers(ring_size, network_type);
            System.out.println("\n>>>>>>>>>>>>>>>> Done");
        }
    }

    private void ringRecovers(final int ring_size, final KeyDistribution network_type) throws IOException, InterruptedException, TimeoutException, UnknownPlatformException, RPCException {

        System.out.println("constructing ring... ");
        final SingleHostNetwork network = new SingleHostNetwork(ring_size, network_type);
        System.out.println("done");

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(network, CHECK_TIMEOUT);
    }
}
