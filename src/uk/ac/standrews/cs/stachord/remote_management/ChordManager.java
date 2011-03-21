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

import uk.ac.standrews.cs.nds.madface.IPingable;
import uk.ac.standrews.cs.nds.p2p.network.P2PNodeFactory;
import uk.ac.standrews.cs.nds.p2p.network.P2PNodeManager;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;

/**
 * Provides remote management hooks for Chord.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ChordManager extends P2PNodeManager {

    private final ChordNodeFactory factory;

    /**
     * Name of 'ring size' attribute.
     */
    public static final String RING_SIZE_NAME = "Ring Size";
    private static final String CHORD_APPLICATION_NAME = "Chord";

    /**
     * Initializes a Chord manager.
     */
    public ChordManager(final boolean local_deployment_only) {

        super(local_deployment_only);

        factory = new ChordNodeFactory();

        getSingleScanners().add(new ChordCycleLengthScanner());
        getGlobalScanners().add(new ChordPartitionScanner());
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public String getApplicationName() {

        return CHORD_APPLICATION_NAME;
    }

    @Override
    public IPingable getApplicationReference(final InetSocketAddress inet_socket_address) throws RPCException {

        return factory.bindToNode(inet_socket_address);
    }

    @Override
    protected P2PNodeFactory getP2PNodeFactory() {

        return factory;
    }
}
