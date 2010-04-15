package uk.ac.standrews.cs.stachordRMI.servers;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;

/**
 * Provides the entry point for deploying a Chord node that creates a new Chord Ring
 *
 * Creates a Chord node that creates a new ring.
 *
 * A single command line parameter is required:
 * <dl>
 * 	<dt>-s[host][:port]</dt>
 * 	<dd>Specifies the host address and port for the local machine on which the Chord services should be made available.
 * 	</dd>
 * </dl>
 * 
 * @author al
 */
public class StartRing extends AbstractServer {

	public static void main(String[] args) throws RemoteException, NotBoundException {

		setup(args);

		Diagnostic.traceNoSource(DiagnosticLevel.FULL, "Starting new RMI Chord ring with address: ", local_address, " on port: ", local_port, " with key: ", server_key);

		startChordRing(local_address, local_port, server_key);
	}

	public static IChordNode startChordRing(String hostname, int port) throws RemoteException, NotBoundException {

		return startChordRing(hostname, port, null);
	}

	public static IChordNode startChordRing(String hostname, int port, IKey node_key) throws RemoteException, NotBoundException {

		InetSocketAddress localChordAddress = new InetSocketAddress(hostname, port);

		if (node_key == null) return new ChordNodeImpl(localChordAddress, null);
		else                  return new ChordNodeImpl(localChordAddress, null, node_key);
	}
}
