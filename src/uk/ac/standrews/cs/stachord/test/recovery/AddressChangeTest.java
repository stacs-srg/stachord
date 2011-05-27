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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Observable;
import java.util.Observer;

import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;

/**
 * Stand-alone test of ability to detect and accommodate change of network interface.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class AddressChangeTest implements Observer {

    private static final int CHORD_PORT = 9091;

    private static IChordNode impl;
    private static InetSocketAddress socketAddress;

    /**
     * Runs a local chord node and outputs diagnostics on address change events.
     *
     * @param args ignored
     * @throws IOException if the node cannot bind to the specified local address
     * @throws RPCException if an error occurs binding the node to the registry
     * @throws AlreadyBoundException if another node is already bound in the registry
     * @throws RegistryUnavailableException if the registry is unavailable
     */
    public static void main(final String[] args) throws IOException, RPCException, AlreadyBoundException, RegistryUnavailableException {

        final AddressChangeTest foo = new AddressChangeTest();

        socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), CHORD_PORT);
        impl = new ChordNodeFactory().createNode(socketAddress);
        impl.addObserver(foo);

        printStatus();
    }

    @Override
    public void update(final Observable o, final Object arg) {

        printStatus();
        System.out.println(arg);
    }

    // -------------------------------------------------------------------------------------------------------------------------------

    private static void printStatus() {

        System.out.println("Running... at " + impl.getSelfReference().getCachedAddress() + " started at: " + socketAddress);
    }
}
