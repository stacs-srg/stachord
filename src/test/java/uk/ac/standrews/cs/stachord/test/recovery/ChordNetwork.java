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

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.p2p.keys.KeyDistribution;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.Timing;
import uk.ac.standrews.cs.shabdiz.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.api.ApplicationManager;
import uk.ac.standrews.cs.shabdiz.p2p.network.Network;
import uk.ac.standrews.cs.shabdiz.p2p.network.P2PNetwork;
import uk.ac.standrews.cs.shabdiz.util.URL;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.remote_management.ChordManager;

/**
 * Network comprising Chord nodes running on a set of specified machines running Linux or OSX.
 * 
 * @author Graham Kirby(graham.kirby@st-andrews.ac.uk)
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 */
public class ChordNetwork implements Network {

    // TODO make variant without network

    private final Network network;

    private static final Duration KNOWN_NODE_CONTACT_RETRY_INTERVAL = new Duration(2, TimeUnit.SECONDS);
    private static final Duration RING_ASSEMBLY_TIMEOUT = new Duration(10, TimeUnit.MINUTES);

    // -------------------------------------------------------------------------------------------------------

    /**
     * Creates a new Chord network.
     * 
     * @param host_descriptors a description of the target host for each Chord node to be created
     * @param key_distribution the required key distribution
     * @throws Exception if there is an error during creation of the network
     */
    public ChordNetwork(final SortedSet<HostDescriptor> host_descriptors, final KeyDistribution key_distribution) throws Exception {

        final boolean local_deployment_only = allLocal(host_descriptors);
        final ApplicationManager application_manager = new ChordManager(local_deployment_only, false, false);

        final Set<URL> application_urls = new HashSet<URL>();

        application_urls.add(new URL("https://builds.cs.st-andrews.ac.uk/job/stachord/lastSuccessfulBuild/artifact/bin/stachord.jar"));
        application_urls.add(new URL("https://builds.cs.st-andrews.ac.uk/job/stachord/lastSuccessfulBuild/artifact/lib/json.jar"));
        application_urls.add(new URL("https://builds.cs.st-andrews.ac.uk/job/stachord/lastSuccessfulBuild/artifact/lib/mindterm.jar"));

        network = new P2PNetwork(host_descriptors, application_manager, application_urls, key_distribution);

        assembleChordRing(host_descriptors);
    }

    protected static void assembleChordRing(final SortedSet<HostDescriptor> host_descriptors) throws InterruptedException, TimeoutException {

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
                final IChordRemoteReference known_node_constant = known_node;

                final Callable<Void> join_action = new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {

                        final IChordRemoteReference node_reference = (IChordRemoteReference) new_node_descriptor.getApplicationReference();

                        // The known node may not have come up yet.
                        if (node_reference == null) { throw new Exception("known node not accessible yet"); }

                        final IChordRemote node = node_reference.getRemote();
                        node.join(known_node_constant);

                        return null;
                    }
                };

                try {
                    Timing.retry(join_action, RING_ASSEMBLY_TIMEOUT, KNOWN_NODE_CONTACT_RETRY_INTERVAL, true, DiagnosticLevel.FULL);
                }
                catch (final Exception e) {
                    launderException(e);
                }
            }
        }
    }

    private static void launderException(final Exception e) throws TimeoutException, InterruptedException {

        if (e instanceof InterruptedException) { throw (InterruptedException) e; }
        if (e instanceof TimeoutException) { throw (TimeoutException) e; }
        if (e instanceof RuntimeException) { throw (RuntimeException) e; }

        throw new IllegalStateException("Unexpected checked exception", e);
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

    @Override
    public void shutdown() {

        network.shutdown();
    }
}
