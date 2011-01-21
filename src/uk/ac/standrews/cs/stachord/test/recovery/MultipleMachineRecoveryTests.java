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

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.test.factory.INetwork;
import uk.ac.standrews.cs.stachord.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachord.test.factory.MultipleHostNetwork;

/**
 * Various tests of small ring recovery, not intended to be run automatically.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class MultipleMachineRecoveryTests {

    private static final String STACHORD_JAR = "http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachord/lastSuccessfulBuild/artifact/bin/stachord.jar";
    private static final int TIMEOUT = 500;

    /**
     * Runs a multiple machine test using password authentication and assuming that libraries are pre-installed on remote machines.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void multiMachineTestPasswordNoLibraryInstallation() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        final List<String> hosts = twoEachOnBeastAndMini();
        final List<ClassPath> class_paths = beastAndMiniClassPaths();

        final List<HostDescriptor> host_descriptors = HostDescriptor.createDescriptorsUsingPassword(hosts, true);
        HostDescriptor.setClassPaths(host_descriptors, class_paths);

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(host_descriptors, KeyDistribution.RANDOM), TIMEOUT);

        System.out.println(">>>>> recovery test completed");
    }

    /**
     * Runs a multiple machine test using public key authentication and assuming that libraries are pre-installed on remote machines.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void multiMachineTestPublicKeyNoLibraryInstallation() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        final List<String> hosts = twoEachOnBeastAndMini();
        final List<ClassPath> class_paths = beastAndMiniClassPaths();

        final List<HostDescriptor> host_descriptors = HostDescriptor.createDescriptorsUsingPublicKey(hosts, true);
        HostDescriptor.setClassPaths(host_descriptors, class_paths);

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(host_descriptors, KeyDistribution.RANDOM), TIMEOUT);

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

        final List<String> hosts = threeOnBeast();

        final URL[] lib_urls = new URL[]{new URL(STACHORD_JAR)};

        final List<HostDescriptor> host_descriptors = HostDescriptor.createDescriptorsUsingPassword(hosts, true);
        HostDescriptor.setApplicationURLs(host_descriptors, lib_urls);

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(host_descriptors, KeyDistribution.RANDOM), TIMEOUT);

        System.out.println(">>>>> recovery test completed");
    }

    /**
     * Runs a multiple machine test using password authentication and dynamically installing libraries on remote machines and the local machine to permit manual disconnection testing
     *
     * @throws Exception if the test fails
     */
    @Test
    public void manualNetworkChangeRecovery() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.FULL);

        final List<String> hosts = threeOnBeast();

        final URL[] lib_urls = new URL[]{new URL(STACHORD_JAR)};

        final List<HostDescriptor> host_descriptors = HostDescriptor.createDescriptorsUsingPassword(hosts, true);

        HostDescriptor.setApplicationURLs(host_descriptors, lib_urls);

        final INetwork network = new MultipleHostNetwork(host_descriptors, KeyDistribution.RANDOM);

        RecoveryTestLogic.waitForStableRing(network.getNodes(), 60000);

        System.out.println("USER: Please change network connection on local node - please hit return");

        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        reader.readLine(); // wait for input

        final HostDescriptor a_beast_node = network.getNodes().get(0);

        assertEquals(a_beast_node.getHost(), "beast.cs.st-andrews.ac.uk");

        RecoveryTestLogic.ringStable(a_beast_node, 4);

        RecoveryTestLogic.dumpState(network.getNodes());

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

        final List<String> hosts = threeBlubNodes();

        final URL[] lib_urls = new URL[]{new URL(STACHORD_JAR)};

        final List<HostDescriptor> host_descriptors = HostDescriptor.createDescriptorsUsingPublicKey(hosts, true);
        HostDescriptor.setApplicationURLs(host_descriptors, lib_urls);

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(host_descriptors, KeyDistribution.RANDOM), TIMEOUT);

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
        hosts.add("mini.cs.st-andrews.ac.uk");
        hosts.add("mini.cs.st-andrews.ac.uk");
        return hosts;
    }

    private List<String> threeOnBeast() {

        final List<String> hosts = new ArrayList<String>();
        hosts.add("beast.cs.st-andrews.ac.uk");
        hosts.add("beast.cs.st-andrews.ac.uk");
        hosts.add("beast.cs.st-andrews.ac.uk");
        return hosts;
    }

    private List<String> threeOnBeastAndOneOnIO() {

        final List<String> hosts = new ArrayList<String>();
        hosts.add("beast.cs.st-andrews.ac.uk");
        hosts.add("beast.cs.st-andrews.ac.uk");
        hosts.add("beast.cs.st-andrews.ac.uk");
        hosts.add("io.cs.st-andrews.ac.uk");
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
