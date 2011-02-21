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

import java.util.List;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.interfaces.IApplicationManager;
import uk.ac.standrews.cs.nds.p2p.network.INetwork;
import uk.ac.standrews.cs.nds.p2p.network.KeyDistribution;
import uk.ac.standrews.cs.nds.p2p.network.MultipleHostNetwork;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.remote_management.ChordManager;

/**
 * Network comprising Chord nodes running on a set of specified physical machines running Linux or OSX.
 *
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 */
public class MultipleHostChordNetwork implements INetwork {

    // TODO make variants without network and with all nodes in same VM using network

    private final INetwork network;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new Chord network.
     *
     * @param host_descriptors a description of the target host for each Chord node to be created
     * @param key_distribution the required key distribution
     * 
     * @throws Exception if there is an error during creation of the network
     */
    public MultipleHostChordNetwork(final List<HostDescriptor> host_descriptors, final KeyDistribution key_distribution) throws Exception {

        final IApplicationManager application_manager = new ChordManager();
        network = new MultipleHostNetwork(host_descriptors, application_manager, key_distribution);

        final HostDescriptor known_node_descriptor = host_descriptors.get(0);
        final IChordRemoteReference known_node = (IChordRemoteReference) known_node_descriptor.getApplicationReference();

        // Join the nodes.
        for (int node_index = 1; node_index < host_descriptors.size(); node_index++) {

            final HostDescriptor new_node_descriptor = host_descriptors.get(node_index);
            final IChordRemote node = ((IChordRemoteReference) new_node_descriptor.getApplicationReference()).getRemote();

            node.join(known_node);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public List<HostDescriptor> getNodes() {

        return network.getNodes();
    }

    @Override
    public void killNode(final HostDescriptor node) throws Exception {

        network.killNode(node);
    }

    @Override
    public void killAllNodes() {

        network.killAllNodes();
    }
}
