package uk.ac.standrews.cs.stachordRMI.servers;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
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

	public static void main( String[] args ) {

		setup(args);

		Diagnostic.traceNoSource(DiagnosticLevel.RUN, "Starting new RMI Chord ring with address: " + local_address + " on port: " + local_port );

		try {
			startChordRing( local_address, local_port );
			
		} catch (RemoteException e) {
			ErrorHandling.exceptionError(e, "starting new RMI Chord ring" );
		} catch (P2PNodeException e) {
			ErrorHandling.exceptionError(e, "starting new RMI Chord ring" );
		}
	}

	public static IChordNode startChordRing(String hostname, int port) throws RemoteException, P2PNodeException {

		InetSocketAddress localChordAddress = new InetSocketAddress(hostname, port);
		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Deploying new Chord ring on " + hostname + ":" + port);

		IChordNode chordNode = ChordNodeImpl.deployNode(localChordAddress, null);

		Diagnostic.traceNoEvent(DiagnosticLevel.FULL, "Started local Chord node on : " + hostname + ":" + port + 
				" : initialized with key :" + chordNode.getKey().toString(10) + " : " + chordNode.getKey()  );

		return chordNode;
	}
}
