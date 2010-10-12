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

package uk.ac.standrews.cs.stachord.test.recovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.SSH2ConnectionWrapper;
import uk.ac.standrews.cs.nds.remote_management.UnknownPlatformException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.test.factory.KeyDistribution;
import uk.ac.standrews.cs.stachord.test.factory.MultipleHostNetwork;
import uk.ac.standrews.cs.stachord.test.factory.NetworkUtil;
import uk.ac.standrews.cs.stachord.test.factory.UnequalArrayLengthsException;

import com.mindbright.ssh2.SSH2Exception;

/**
 * Various tests of ring recovery on the Ganglia cluster, not intended to be run automatically.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class GangliaRecoveryTests {

	/**
	 * Runs a multiple machine test using public key authentication and dynamically installing libraries on remote machines.
	 *
	 * @throws IOException
	 * @throws NotBoundException
	 * @throws SSH2Exception
	 * @throws UnequalArrayLengthsException
	 * @throws InterruptedException
	 * @throws TimeoutException
	 * @throws UnknownPlatformException
	 */
	@Test
	public void gangliaTestPublicKeyLibraryInstallation() throws IOException, NotBoundException, SSH2Exception, UnequalArrayLengthsException, InterruptedException, TimeoutException, UnknownPlatformException {

		Diagnostic.setLevel(DiagnosticLevel.NONE);

		URL[] lib_urls = new URL[] {
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/nds/lastStableBuild/artifact/bin/nds.jar"),
			new URL("http://www-systems.cs.st-andrews.ac.uk:8080/hudson/job/stachordRMI/lastStableBuild/artifact/bin/stachordRMI.jar")
		};

		List<InetAddress> addresses = getGangliaNodeAddresses();

		List<SSH2ConnectionWrapper> connections = NetworkUtil.createPublicKeyConnections(addresses, true);
		List<HostDescriptor> node_descriptors = NetworkUtil.createHostDescriptors(connections, lib_urls);

		RecoveryTestLogic.testRingRecoveryFromNodeFailure(new MultipleHostNetwork(node_descriptors, KeyDistribution.RANDOM), 500);

		System.out.println(">>>>> recovery test completed");
	}

	protected List<InetAddress> getGangliaNodeAddresses() throws UnknownHostException {

		List<InetAddress> address_list = new ArrayList<InetAddress>();

		for (int index = 0; index <= 57; index++) {
			address_list.add(InetAddress.getByName("compute-0-" + index));
		}

		// Remove bad nodes.
		address_list.remove(InetAddress.getByName("compute-0-42"));
		address_list.remove(InetAddress.getByName("compute-0-46"));
		address_list.remove(InetAddress.getByName("compute-0-53"));

		return address_list;
	}
}
