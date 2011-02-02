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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachord.test.factory.MultipleHostNetwork;

/**
 * Various tests of ring recovery on the Ganglia cluster, not intended to be run automatically.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class GangliaRecoveryTests {

    private static final int NO_OF_NODES = 57;
    private static final int TIMEOUT = 500;

    /**
     * Runs a multiple machine test using public key authentication and dynamically installing libraries on remote machines.
     *
     * @throws Exception if the test fails
     */
    @Test
    public void gangliaTestPublicKeyLibraryInstallation() throws Exception {

        Diagnostic.setLevel(DiagnosticLevel.NONE);

        final URL[] lib_urls = new URL[]{new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachord/lastSuccessfulBuild/artifact/bin/stachord.jar")};

        final List<String> hosts = getGangliaNodeAddresses();
        final List<HostDescriptor> host_descriptors = HostDescriptor.createDescriptorsUsingPublicKey(hosts, true);
        HostDescriptor.setApplicationURLs(host_descriptors, lib_urls);

        RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(host_descriptors, KeyDistribution.RANDOM), TIMEOUT);

        System.out.println(">>>>> recovery test completed");
    }

    protected List<String> getGangliaNodeAddresses() {

        final List<String> address_list = new ArrayList<String>();

        for (int index = 0; index <= NO_OF_NODES; index++) {
            address_list.add("compute-0-" + index);
        }

        // Remove bad nodes.
        address_list.remove("compute-0-42");
        address_list.remove("compute-0-46");
        address_list.remove("compute-0-53");

        return address_list;
    }
}
