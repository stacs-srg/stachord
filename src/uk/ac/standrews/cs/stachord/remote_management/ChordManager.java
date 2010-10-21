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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.IApplicationManager;
import uk.ac.standrews.cs.nds.remote_management.IGlobalHostScanner;
import uk.ac.standrews.cs.nds.remote_management.ISingleHostScanner;
import uk.ac.standrews.cs.nds.remote_management.ProcessInvocation;
import uk.ac.standrews.cs.nds.util.IActionWithNoResult;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.Timeout;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.servers.StartNodeInNewRing;

/**
 * Provides remote management hooks for Chord.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class ChordManager implements IApplicationManager {

    // private static final String AUTO_DISCOVER_CHECKBOX_LABEL = "Auto-Discover";
    // private static final String AUTO_ADD_CHECKBOX_LABEL =      "Auto-Add Other Ring Hosts";

    private static final String CHORD_APPLICATION_CLASSNAME = StartNodeInNewRing.class.getCanonicalName(); // Full name of the class used to instantiate a Chord ring.
    private static final int APPLICATION_CALL_TIMEOUT = 10000; // The timeout for attempted application calls, in ms.
    private static final String CHORD_APPLICATION_NAME = "Chord";
    static final String RING_SIZE_NAME = "Ring Size";

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void attemptApplicationCall(final HostDescriptor host_descriptor) throws Exception {

        host_descriptor.setPort(IChordNode.DEFAULT_RMI_REGISTRY_PORT);

        // Try to connect to the application on the default RMI port.
        final InetSocketAddress inet_socket_address = NetworkUtil.getInetSocketAddress(host_descriptor.getHost(), host_descriptor.getPort());

        // Wrap the exception variable so that it can be updated by the timeout thread.
        final Exception[] exception_wrapper = new Exception[]{null};

        // Try to connect to the application, subject to a timeout.
        new Timeout().performActionWithTimeout(new IActionWithNoResult() {

            @Override
            public void performAction() {

                try {
                    // Try to access the application at the specified address.
                    host_descriptor.setApplicationReference(ChordNodeFactory.bindToRemoteNode(inet_socket_address));
                }
                catch (final Exception e) {
                    // We have to store the exception here for later access, rather than throwing it, since an ActionWithNoResult can't throw exceptions and anyway
                    // it's being executed in the timeout thread.
                    exception_wrapper[0] = e;
                }
            }

        }, APPLICATION_CALL_TIMEOUT);

        // The exception will be null if the application call succeeded.
        final Exception e = exception_wrapper[0];
        if (e != null) { throw e; }
    }

    @Override
    public void deployApplication(final HostDescriptor host_descriptor) throws Exception {

        host_descriptor.setProcess(ChordNodeFactory.instantiateRemoteNode(host_descriptor));
    }

    @Override
    public void killApplication(final HostDescriptor host_descriptor) throws Exception {

        // Although the host descriptor may contain a process handle, we don't use it for killing off the application,
        // because it's possible that it refers to a dead process while there is another live process.
        // This can happen when the application is deployed but the status scanner doesn't notice that it's live
        // before the deploy scanner has another attempt to deploy it. In this case the process handle in the host
        // descriptor will refer to the second process, but that will have died immediately due to the port being
        // bound to the first one.

        // For simplicity we just kill all Chord nodes. Obviously this won't work in situations where multiple
        // Chord nodes are being run on the same machine.

        ProcessInvocation.killMatchingProcesses(CHORD_APPLICATION_CLASSNAME, host_descriptor.getSSHClientWrapper());
    }

    @Override
    public String getApplicationName() {

        return CHORD_APPLICATION_NAME;
    }

    @Override
    public List<ISingleHostScanner> getSingleScanners() {

        final List<ISingleHostScanner> result = new ArrayList<ISingleHostScanner>();
        result.add(new ChordCycleLengthScanner());

        return result;
    }

    @Override
    public List<IGlobalHostScanner> getGlobalScanners() {

        final List<IGlobalHostScanner> result = new ArrayList<IGlobalHostScanner>();
        result.add(new ChordPartitionScanner());

        return result;
    }
}