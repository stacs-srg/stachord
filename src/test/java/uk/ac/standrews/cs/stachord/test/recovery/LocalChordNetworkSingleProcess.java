/***************************************************************************
 *                                                                         *
 * nds Library                                                             *
 * Copyright (C) 2005-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of nds, a package of utility classes.                 *
 *                                                                         *
 * nds is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * nds is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with nds.  If not, see <http://www.gnu.org/licenses/>.            *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.stachord.test.recovery;

import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.shabdiz.legacy.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.legacy.p2p.network.Network;
import uk.ac.standrews.cs.stachord.impl.ChordLocalReference;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;

/**
 * Network comprising Chord nodes all running on the local machine, in the same process.
 * This creates the node instances as local objects, via {@link ChordNodeFactory}.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 */
public class LocalChordNetworkSingleProcess implements Network {

    private final SortedSet<HostDescriptor> host_descriptors;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new network.
     *
     * @param number_of_nodes the number of nodes to be created
     * @param key_distribution the required key distribution
     * @throws Exception if there is an error during creation of the network
     */
    public LocalChordNetworkSingleProcess(final int number_of_nodes, final KeyDistribution key_distribution) throws Exception {

        host_descriptors = new ConcurrentSkipListSet<HostDescriptor>();

        final IKey[] node_keys = key_distribution.generateKeys(number_of_nodes);

        final ChordNodeFactory factory = new ChordNodeFactory();

        for (int node_index = 0; node_index < number_of_nodes; node_index++) {

            final HostDescriptor host_descriptor = new HostDescriptor();

            host_descriptor.applicationDeploymentParams(new Object[]{node_keys[node_index]});
            host_descriptor.deployInLocalProcess(true);

            // Set the application_reference field in the host descriptor to an instance of ChordLocalReference, controlled by the deployInLocalProcess() flag.
            factory.createNode(host_descriptor, node_keys[node_index]);

            host_descriptors.add(host_descriptor);
        }

        // Join the nodes to each other.
        ChordNetwork.assembleChordRing(host_descriptors);
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public SortedSet<HostDescriptor> getNodes() {

        return host_descriptors;
    }

    /**
     * Simulates node failure by shutting down the node's network server.
     * @param host_descriptor
     * @throws Exception
     */
    private void shutdownNode(final HostDescriptor host_descriptor) throws Exception {

        final ChordLocalReference applicationReference = (ChordLocalReference) host_descriptor.getApplicationReference();
        final IChordNode node = applicationReference.getNode();
        node.shutDown();
    }

    @Override
    public synchronized void killNode(final HostDescriptor host_descriptor) throws Exception {

        shutdownNode(host_descriptor);
        host_descriptors.remove(host_descriptor);
    }

    @Override
    public synchronized void killAllNodes() throws Exception {

        for (final HostDescriptor host_descriptor : host_descriptors) {
            shutdownNode(host_descriptor);
        }
        host_descriptors.clear();
    }

    @Override
    public void shutdown() {

    }
}
