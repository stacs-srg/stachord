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

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.HostState;
import uk.ac.standrews.cs.nds.remote_management.IGlobalHostScanner;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.interfaces.RemoteException;

class ChordPartitionScanner implements IGlobalHostScanner {

    private static final int MIN_CYCLE_TIME = 20000;
    private boolean enabled = false;

    @Override
    public int getMinCycleTime() {

        return MIN_CYCLE_TIME;
    }

    @Override
    public void check(final List<HostDescriptor> host_descriptors) {

        if (enabled) {

            // It's possible for the size of the host list, or the entries within it, to change during this method.
            // This shouldn't matter - the worst that can happen is that a node is joined to a ring it's already in,
            // which will have no effect.

            // Gather the running nodes that see complete cycles (non-zero recorded cycle length).
            // If any running nodes are not stable in this way then give up.
            final List<HostDescriptor> stable_hosts = new ArrayList<HostDescriptor>();
            for (final HostDescriptor host_descriptor : host_descriptors) {

                if (host_descriptor.getHostState() == HostState.RUNNING) {

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

                for (int i = 1; i < stable_hosts.size(); i++) {
                    final HostDescriptor host_descriptor = stable_hosts.get(i);
                    final IChordRemote node = ((IChordRemoteReference) host_descriptor.getApplicationReference()).getRemote();
                    try {
                        if (ringSize(host_descriptor) < stable_hosts.size()) {
                            System.out.println("joining " + node.getAddress() + " to " + first_node.getCachedAddress());

                            node.join(first_node);
                        }
                    }
                    catch (final RemoteException e) {
                        Diagnostic.trace(DiagnosticLevel.FULL, "error joining rings");
                    }
                }
            }
        }
    }

    @Override
    public String getToggleLabel() {

        return "Auto-Heal Partitions";
    }

    @Override
    public void setEnabled(final boolean enabled) {

        this.enabled = enabled;
    }

    private int ringSize(final HostDescriptor host_descriptor) {

        final String ring_size_record = host_descriptor.getScanResults().get(ChordManager.RING_SIZE_NAME);
        return ring_size_record != null && !ring_size_record.equals("-") ? Integer.parseInt(ring_size_record) : 0;
    }
}
