/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2011 Distributed Systems Architecture Research Group *
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

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;

import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.Input;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.credentials.SSHPublicKeyCredential;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.SSHHost;
import uk.ac.standrews.cs.shabdiz.legacy.ClassPath;

/**
 * Various tests of small ring recovery, not intended to be run automatically.
 * 
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class MultipleHostRecoveryTests {

    private static final Duration TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static SSHPublicKeyCredential credential;

    @BeforeClass
    public static void setUp() throws Exception {

        credential = SSHPublicKeyCredential.getDefaultRSACredentials(Input.readPassword("Please enter the public key passphrase:"));
    }

    /**
     * Runs a multiple machine test using password authentication and assuming that libraries are pre-installed on remote machines.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void multiMachineTestPasswordNoLibraryInstallation() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        final List<String> host_names = twoEachOnBeastAndMini();
        final Set<Host> hosts = initHostsFromHostNames(host_names);
        final Duration ring_creation_start_time = Duration.elapsed();
        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostChordNetwork(hosts, KeyDistribution.RANDOM), TIMEOUT, ring_creation_start_time);

        System.out.println(">>>>> recovery test completed");
    }

    private Set<Host> initHostsFromHostNames(final List<String> host_names) throws IOException {

        final Set<Host> hosts = new HashSet<Host>();
        for (final String host_name : host_names) {
            final SSHHost host = new SSHHost(host_name, credential);
            hosts.add(host);
        }
        return hosts;
    }

    /**
     * Runs a multiple machine test using public key authentication and assuming that libraries are pre-installed on remote machines.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void multiMachineTestPublicKeyNoLibraryInstallation() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        final List<String> host_names = twoEachOnBeastAndMini();
        final Set<Host> hosts = initHostsFromHostNames(host_names);
        final Duration ring_creation_start_time = Duration.elapsed();
        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostChordNetwork(hosts, KeyDistribution.RANDOM), TIMEOUT, ring_creation_start_time);

        System.out.println(">>>>> recovery test completed");
    }

    /**
     * Runs a multiple machine test using password authentication and dynamically installing libraries on remote machines.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void multiMachineTestPasswordLibraryInstallation() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        final List<String> host_names = threeOnBeast();
        final Set<Host> hosts = initHostsFromHostNames(host_names);
        final Duration ring_creation_start_time = Duration.elapsed();
        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostChordNetwork(hosts, KeyDistribution.RANDOM), TIMEOUT, ring_creation_start_time);

        System.out.println(">>>>> recovery test completed");
    }

    /**
     * Runs a multiple machine test using password authentication and dynamically installing libraries on remote machines and the local machine to permit manual disconnection testing.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void manualNetworkChangeRecovery() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.FULL);

        final List<String> host_names = threeOnBeast();
        final Set<Host> hosts = initHostsFromHostNames(host_names);
        final MultipleHostChordNetwork network = new MultipleHostChordNetwork(hosts, KeyDistribution.RANDOM);

        final Duration test_timeout = new Duration(60000, TimeUnit.MILLISECONDS);
        RecoveryTestLogic.waitForStableRing(network, test_timeout);

        System.out.println("USER: Please change network connection on local node - please hit return");

        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        reader.readLine(); // wait for input

        final ApplicationDescriptor a_beast_node = network.first();

        assertEquals(a_beast_node.getHost(), "beast.cs.st-andrews.ac.uk");

        final int network_size = 4;
        RecoveryTestLogic.ringStable(a_beast_node, network_size);

        RecoveryTestLogic.dumpState(network);

        System.out.println(">>>>> recovery test completed");
    }

    /**
     * Runs a multiple machine test using public key authentication and dynamically installing libraries on remote machines.
     * 
     * @throws Exception if the test fails
     */
    @Test
    public void multiMachineTestPublicKeyLibraryInstallation() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        final List<String> host_names = threeBlubNodes();
        final Set<Host> hosts = initHostsFromHostNames(host_names);
        final Duration ring_creation_start_time = Duration.elapsed();
        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostChordNetwork(hosts, KeyDistribution.RANDOM), TIMEOUT, ring_creation_start_time);

        System.out.println(">>>>> recovery test completed");
    }

    private List<ClassPath> beastAndMiniClassPaths() {

        final List<ClassPath> class_paths = new ArrayList<ClassPath>();
        class_paths.add(new ClassPath("/usr/share/hudson/jobs/stachord/lastSuccessful/archive/bin/stachord.jar"));
        class_paths.add(new ClassPath("/usr/share/hudson/jobs/stachord/lastSuccessful/archive/bin/stachord.jar"));
        class_paths.add(new ClassPath("/Users/graham/stachord.jar"));
        class_paths.add(new ClassPath("/Users/graham/stachord.jar"));
        return class_paths;
    }

    private List<String> twoEachOnBeastAndMini() {

        final List<String> hosts = new ArrayList<String>();
        hosts.add("beast.cs.st-andrews.ac.uk");
        hosts.add("beast.cs.st-andrews.ac.uk");
        // hosts.add("mini.cs.st-andrews.ac.uk");
        // hosts.add("mini.cs.st-andrews.ac.uk");
        return hosts;
    }

    private List<String> threeOnBeast() {

        final List<String> hosts = new ArrayList<String>();
        hosts.add("beast.cs.st-andrews.ac.uk");
        hosts.add("beast.cs.st-andrews.ac.uk");
        hosts.add("beast.cs.st-andrews.ac.uk");
        return hosts;
    }

    private List<String> threeBlubNodes() {

        final List<String> hosts = new ArrayList<String>();
        hosts.add("compute-0-33");
        hosts.add("compute-0-34");
        hosts.add("compute-0-35");
        return hosts;
    }
}
