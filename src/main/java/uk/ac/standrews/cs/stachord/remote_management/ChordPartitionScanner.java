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

package uk.ac.standrews.cs.stachord.remote_management;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.DefaultMadfaceManager;
import uk.ac.standrews.cs.shabdiz.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.api.State;
import uk.ac.standrews.cs.shabdiz.scanners.AbstractHostScanner;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

class ChordPartitionScanner extends AbstractHostScanner {

    private static final Duration CYCLE_LENGTH_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    public ChordPartitionScanner(final DefaultMadfaceManager manager, final int thread_pool_size, final Duration min_cycle_time) {

        super(manager, min_cycle_time, CYCLE_LENGTH_CHECK_TIMEOUT, "partition scanner", false);
    }

    @Override
    public String getName() {

        return "Partition";
    }

    @Override
    public String getToggleLabel() {

        return "Auto-Heal Partitions";
    }

    private int ringSize(final HostDescriptor host_descriptor) {

        try {
            return ChordMonitoring.cycleLengthFrom(host_descriptor, true);
        }
        catch (final InterruptedException e) {
            return 0;
        }
    }

    @Override
    public void scan(final Set<HostDescriptor> host_descriptors) {

        if (isEnabled()) {

            // It's possible for the size of the host list, or the entries within it, to change during this method.
            // This shouldn't matter - the worst that can happen is that a node is joined to a ring it's already in,
            // which will have no effect.

            // Gather the running nodes that see complete cycles (non-zero recorded cycle length).
            // If any running nodes are not stable in this way then give up.
            final List<HostDescriptor> stable_hosts = new ArrayList<HostDescriptor>();
            for (final HostDescriptor host_descriptor : host_descriptors) {

                if (host_descriptor.getHostState() == State.RUNNING) {

                    if (ringSize(host_descriptor) > 0) {
                        stable_hosts.add(host_descriptor);
                    }
                    else {
                        return;
                    }
                }
            }

            // For each stable node with a cycle length less than the number of stable nodes, join it to the first node.
            if (stable_hosts.size() > 1) {

                final IChordRemoteReference first_node = (IChordRemoteReference) stable_hosts.get(0).getApplicationReference();

                if (first_node != null) {
                    for (int i = 1; i < stable_hosts.size(); i++) {
                        final HostDescriptor host_descriptor = stable_hosts.get(i);
                        final IChordRemoteReference remote_reference = (IChordRemoteReference) host_descriptor.getApplicationReference();
                        if (remote_reference != null) {
                            final IChordRemote node = remote_reference.getRemote();
                            try {
                                if (ringSize(host_descriptor) < stable_hosts.size()) {

                                    node.join(first_node);
                                }
                            }
                            catch (final RPCException e) {
                                Diagnostic.trace(DiagnosticLevel.FULL, "error joining rings");
                            }
                        }
                    }
                }
            }
        }

    }

}
