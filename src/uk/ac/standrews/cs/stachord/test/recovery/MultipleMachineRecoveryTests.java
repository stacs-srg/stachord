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

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.NetworkUtil;
import uk.ac.standrews.cs.nds.remote_management.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachord.test.factory.MultipleHostNetwork;

/**
 * Various tests of small ring recovery, not intended to be run automatically.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class MultipleMachineRecoveryTests {

    private static final int TIMEOUT = 500;

    /**
     * Runs a multiple machine test using password authentication and assuming that libraries are pre-installed on remote machines.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void multiMachineTestPasswordNoLibraryInstallation() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        final List<InetAddress> addresses = twoEachOnBeastAndMini();

        final List<ClassPath> class_paths = beastAndMiniClassPaths();

        final List<SSH2ConnectionWrapper> connections = NetworkUtil.createUsernamePasswordConnections(addresses, true);
        final List<HostDescriptor> node_descriptors = NetworkUtil.createHostDescriptors(connections, class_paths);

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(node_descriptors, KeyDistribution.RANDOM), TIMEOUT);

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

        final List<InetAddress> addresses = twoEachOnBeastAndMini();

        final List<ClassPath> class_paths = beastAndMiniClassPaths();

        final List<SSH2ConnectionWrapper> connections = NetworkUtil.createPublicKeyConnections(addresses, true);
        final List<HostDescriptor> node_descriptors = NetworkUtil.createHostDescriptors(connections, class_paths);

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(node_descriptors, KeyDistribution.RANDOM), TIMEOUT);

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

        final List<InetAddress> addresses = twoEachOnBeastAndMini();

        final URL[] lib_urls = new URL[]{new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"), new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/remote_management/lastStableBuild/artifact/bin/remote_management.jar"), new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar")};

        final List<SSH2ConnectionWrapper> connections = NetworkUtil.createUsernamePasswordConnections(addresses, true);
        final List<HostDescriptor> node_descriptors = NetworkUtil.createHostDescriptors(connections, lib_urls);

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(node_descriptors, KeyDistribution.RANDOM), TIMEOUT);

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

        final List<InetAddress> addresses = new ArrayList<InetAddress>();
        addresses.add(InetAddress.getByName("compute-0-33"));
        addresses.add(InetAddress.getByName("compute-0-34"));
        addresses.add(InetAddress.getByName("compute-0-35"));

        final URL[] lib_urls = new URL[]{new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"), new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/remote_management/lastStableBuild/artifact/bin/remote_management.jar"), new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar")};

        final List<SSH2ConnectionWrapper> connections = NetworkUtil.createPublicKeyConnections(addresses, true);
        final List<HostDescriptor> node_descriptors = NetworkUtil.createHostDescriptors(connections, lib_urls);

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(node_descriptors, KeyDistribution.RANDOM), TIMEOUT);

        System.out.println(">>>>> recovery test completed");
    }

    private List<ClassPath> beastAndMiniClassPaths() {

        final List<ClassPath> class_paths = new ArrayList<ClassPath>();
        class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/remote_management/lastStable/archive/bin/remote_management.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
        class_paths.add(new ClassPath("/usr/share/hudson/jobs/nds/lastStable/archive/bin/nds.jar:/usr/share/hudson/jobs/remote_management/lastStable/archive/bin/remote_management.jar:/usr/share/hudson/jobs/stachordRMI/lastStable/archive/bin/stachordRMI.jar"));
        class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/remote_management.jar:/Users/graham/stachordRMI.jar"));
        class_paths.add(new ClassPath("/Users/graham/nds.jar:/Users/graham/remote_management.jar:/Users/graham/stachordRMI.jar"));
        return class_paths;
    }

    private List<InetAddress> twoEachOnBeastAndMini() throws UnknownHostException {

        final List<InetAddress> addresses = new ArrayList<InetAddress>();
        addresses.add(InetAddress.getByName("beast.cs.st-andrews.ac.uk"));
        addresses.add(InetAddress.getByName("beast.cs.st-andrews.ac.uk"));
        addresses.add(InetAddress.getByName("mini.cs.st-andrews.ac.uk"));
        addresses.add(InetAddress.getByName("mini.cs.st-andrews.ac.uk"));
        return addresses;
    }
}
