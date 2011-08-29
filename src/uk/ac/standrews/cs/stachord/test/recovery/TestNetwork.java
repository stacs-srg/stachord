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

package uk.ac.standrews.cs.stachord.test.recovery;

import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.p2p.network.INetwork;
import uk.ac.standrews.cs.nds.p2p.network.KeyDistribution;

/**
 * Network comprising P2P nodes all running on the local machine.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 */
public class TestNetwork implements INetwork {

    private final INetwork network;

    /**
     * Creates a new network.
     *
     * @param number_of_nodes the number of nodes to be created
     * @param key_distribution the required key distribution
     * @throws Exception if there is an error during creation of the network
     */
    public TestNetwork(final int number_of_nodes, final KeyDistribution key_distribution) throws Exception {

        final SortedSet<HostDescriptor> node_descriptors = new ConcurrentSkipListSet<HostDescriptor>();

        for (int i = 0; i < number_of_nodes; i++) {
            final HostDescriptor hostDescriptor = new HostDescriptor();
            node_descriptors.add(hostDescriptor);
        }

        network = new ChordNetwork(node_descriptors, key_distribution);
    }

    @Override
    public SortedSet<HostDescriptor> getNodes() {

        return network.getNodes();
    }

    @Override
    public void killNode(final HostDescriptor node) throws Exception {

        network.killNode(node);
    }

    @Override
    public void killAllNodes() throws Exception {

        network.killAllNodes();
    }

    @Override
    public void shutdown() {

        network.shutdown();
    }
}
