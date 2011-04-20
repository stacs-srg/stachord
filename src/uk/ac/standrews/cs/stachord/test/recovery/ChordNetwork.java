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

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.interfaces.IApplicationManager;
import uk.ac.standrews.cs.nds.p2p.network.INetwork;
import uk.ac.standrews.cs.nds.p2p.network.KeyDistribution;
import uk.ac.standrews.cs.nds.p2p.network.P2PNetwork;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.remote_management.ChordManager;

/**
 * Network comprising Chord nodes running on a set of specified machines running Linux or OSX.
 *
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 */
public class ChordNetwork implements INetwork {

    // TODO make variant without network

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
    public ChordNetwork(final SortedSet<HostDescriptor> host_descriptors, final KeyDistribution key_distribution) throws Exception {

        final boolean local_deployment_only = allLocal(host_descriptors);
        final IApplicationManager application_manager = new ChordManager(local_deployment_only);
        network = new P2PNetwork(host_descriptors, application_manager, key_distribution);

        assembleChordRing(host_descriptors);
    }

    protected static void assembleChordRing(final SortedSet<HostDescriptor> host_descriptors) {

        HostDescriptor known_node_descriptor = null;
        IChordRemoteReference known_node = null;

        for (final HostDescriptor new_node_descriptor : host_descriptors) {

            if (known_node_descriptor == null) {
                // Pick one node for the others to join.

                known_node_descriptor = new_node_descriptor;
                known_node = (IChordRemoteReference) known_node_descriptor.getApplicationReference();
            }
            else {
                // Join the other nodes to the ring via the first one.
                final IChordRemote node = ((IChordRemoteReference) new_node_descriptor.getApplicationReference()).getRemote();

                while (true) {
                    try {
                        node.join(known_node);
                        break;
                    }
                    catch (final RPCException e) {
                        // Retry.
                        Thread.yield();
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------------------------------------

    private boolean allLocal(final SortedSet<HostDescriptor> host_descriptors) {

        for (final HostDescriptor host_descriptor : host_descriptors) {
            if (!host_descriptor.local()) { return false; }
        }
        return true;
    }
}
