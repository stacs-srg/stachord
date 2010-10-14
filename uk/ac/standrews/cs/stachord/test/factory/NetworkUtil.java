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

package uk.ac.standrews.cs.stachord.test.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.MaskedStringInput;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Provides various SSH-related utility methods.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class NetworkUtil {

    /**
     * Creates a list of interactively entered username/password credentials for the specified addresses. Optionally the same credentials can be used for all addresses.
     *
     * @param addresses the addresses
     * @param same_credentials_for_all true if the same credentials should be used for all addresses
     * @return a list of username/password credentials
     *
     * @throws IOException if an error occurs when trying to read in a username or password
     */
    public static List<SSH2ConnectionWrapper> createUsernamePasswordConnections(final List<InetAddress> addresses, final boolean same_credentials_for_all) throws IOException {

        final List<SSH2ConnectionWrapper> connections = new ArrayList<SSH2ConnectionWrapper>();
        connections.add(createUsernamePasswordConnection(addresses.get(0), null));
        final SSH2ConnectionWrapper credentials_to_be_copied = same_credentials_for_all ? connections.get(0) : null;

        for (int i = 1; i < addresses.size(); i++) {
            connections.add(createUsernamePasswordConnection(addresses.get(i), credentials_to_be_copied));
        }
        return connections;
    }

    /**
     * Creates a list of interactively entered username/public key credentials for the specified addresses. Optionally the same credentials can be used for all addresses.
     *
     * @param addresses the addresses
     * @param same_credentials_for_all true if the same credentials should be used for all addresses
     * @return a list of username/password credentials
     *
     * @throws IOException if an error occurs when trying to read in a username or password
     */
    public static List<SSH2ConnectionWrapper> createPublicKeyConnections(final List<InetAddress> addresses, final boolean same_credentials_for_all) throws IOException {

        final List<SSH2ConnectionWrapper> connections = new ArrayList<SSH2ConnectionWrapper>();
        connections.add(createPublicKeyConnection(addresses.get(0), null));
        final SSH2ConnectionWrapper credentials_to_be_copied = same_credentials_for_all ? connections.get(0) : null;

        for (int i = 1; i < addresses.size(); i++) {
            connections.add(createPublicKeyConnection(addresses.get(i), credentials_to_be_copied));
        }
        return connections;
    }

    /**
     * Creates a list of host descriptors given a list of SSH credentials and corresponding class paths.
     *
     * @param connections the SSH credentials
     * @param class_paths the corresponding class paths
     * @return a list of host descriptors
     *
     * @throws UnequalArrayLengthsException if the lengths of the two lists are different
     * @throws SSH2Exception if an error occurs when attempting to contact one of the specified remote hosts
     */
    public static List<HostDescriptor> createHostDescriptors(final List<SSH2ConnectionWrapper> connections, final List<ClassPath> class_paths) throws UnequalArrayLengthsException, SSH2Exception {

        checkEqualLengths(connections, class_paths);

        final List<HostDescriptor> node_descriptors = new ArrayList<HostDescriptor>();

        int i = 0;
        for (final SSH2ConnectionWrapper connection : connections) {

            node_descriptors.add(new HostDescriptor(connection, 0, class_paths.get(i)));
            i++;
        }
        return node_descriptors;
    }

    /**
     * Creates a list of host descriptors given a list of SSH credentials and a list of URLs from which class path entries can be obtained.
     *
     * @param connections the SSH credentials
     * @param lib_urls the class path entry URLs
     * @return a list of host descriptors
     *
     * @throws SSH2Exception if an error occurs when attempting to contact one of the specified remote hosts
     */
    public static List<HostDescriptor> createHostDescriptors(final List<SSH2ConnectionWrapper> connections, final URL[] lib_urls) throws SSH2Exception {

        final List<HostDescriptor> node_descriptors = new ArrayList<HostDescriptor>();

        int i = 0;
        for (final SSH2ConnectionWrapper connection : connections) {

            node_descriptors.add(new HostDescriptor(connection, 0, lib_urls));
            i++;
        }

        return node_descriptors;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static SSH2ConnectionWrapper createUsernamePasswordConnection(final InetAddress address, final SSH2ConnectionWrapper credentials_to_be_copied) throws IOException {

        if (credentials_to_be_copied == null) {

            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("enter remote username for " + address.getCanonicalHostName() + ": ");
            final String username = reader.readLine();

            final String password = MaskedStringInput.getMaskedString("enter remote password");

            return new SSH2ConnectionWrapper(address, username, password);
        }

        return new SSH2ConnectionWrapper(address, credentials_to_be_copied.getUserName(), credentials_to_be_copied.getPassword());
    }

    private static SSH2ConnectionWrapper createPublicKeyConnection(final InetAddress address, final SSH2ConnectionWrapper credentials_to_be_copied) throws IOException {

        if (credentials_to_be_copied == null) {

            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            System.out.print("enter remote username for " + address.getCanonicalHostName() + ": ");
            final String username = reader.readLine();

            final String private_key_file_path = new File(new File(System.getProperty("user.home"), ".ssh"), "id_rsa").getAbsolutePath();
            final String pass_phrase = MaskedStringInput.getMaskedString("enter SSH passphrase");

            return new SSH2ConnectionWrapper(address, username, private_key_file_path, pass_phrase);
        }

        return new SSH2ConnectionWrapper(address, credentials_to_be_copied.getUserName(), credentials_to_be_copied.getKeyFile(), credentials_to_be_copied.getKeyPassphrase());
    }

    private static void checkEqualLengths(final List<?>... lists) throws UnequalArrayLengthsException {

        final int first_length = lists[0].size();

        for (final List<?> list : lists) {
            if (list.size() != first_length) { throw new UnequalArrayLengthsException(); }
        }
    }
}
