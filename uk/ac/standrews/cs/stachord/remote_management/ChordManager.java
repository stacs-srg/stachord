package uk.ac.standrews.cs.stachord.remote_management;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.IApplicationManager;
import uk.ac.standrews.cs.nds.remote_management.ProcessInvocation;
import uk.ac.standrews.cs.nds.util.ActionWithNoResult;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.Timeout;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.impl.Constants;
import uk.ac.standrews.cs.stachord.servers.StartNodeInNewRing;
import uk.ac.standrews.cs.stachord.test.factory.MultipleMachineNetwork;

/**
 * Provides remote management hooks for Chord.
 * 
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class ChordManager implements IApplicationManager {

	private static final String CHORD_APPLICATION_CLASSNAME = StartNodeInNewRing.class.getCanonicalName();   // Full name of the class used to instantiate a Chord ring.
	private static final int APPLICATION_CALL_TIMEOUT = 10000;                                               // The timeout for attempted application calls, in ms.

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void attemptApplicationCall(final HostDescriptor host_descriptor) throws Exception {
		
		// Try to connect to the application on the default RMI port.
		final InetSocketAddress inet_socket_address = NetworkUtil.getInetSocketAddress(host_descriptor.host, Constants.DEFAULT_RMI_REGISTRY_PORT);
		
		// Wrap the exception variable so that it can be updated by the timeout thread.
		final Exception[] exception_wrapper = new Exception[] {null};
		
		// Try to connect to the application, subject to a timeout.
		new Timeout().performActionWithTimeout(new ActionWithNoResult() {

			@Override
			public void performAction() {
				try {
					// Try to access the application at the specified address.
					host_descriptor.application_reference = ChordNodeFactory.bindToNode(inet_socket_address);
				}
				catch (Exception e) {
					// We have to store the exception here for later access, rather than throwing it, since an ActionWithNoResult can't throw exceptions and anyway
					// it's being executed in the timeout thread.
					exception_wrapper[0] = e;
				}
			}
			
		}, APPLICATION_CALL_TIMEOUT);

		// The exception will be null if the application call succeeded.
		Exception e = exception_wrapper[0];
		if (e != null) throw e;
	}

	@Override
	public void deployApplication(HostDescriptor host_descriptor) throws Exception {

		MultipleMachineNetwork.createSingleNode(host_descriptor, Constants.DEFAULT_RMI_REGISTRY_PORT);
	}

	@Override
	public void killApplication(HostDescriptor host_descriptor) {
		
		// Although the host descriptor may contain a process handle, we don't use it for killing off the application,
		// because it's possible that it refers to a dead process while there is another live process.
		// This can happen when the application is deployed but the status scanner doesn't notice that it's live
		// before the deploy scanner has another attempt to deploy it. In this case the process handle in the host
		// descriptor will refer to the second process, but that will have died immediately due to the port being
		// bound to the first one.
		
		// For simplicity we just kill all Chord nodes. Obviously this won't work in situations where multiple
		// Chord nodes are being run on the same machine.
		
		try {
			ProcessInvocation.killMatchingProcesses(CHORD_APPLICATION_CLASSNAME, host_descriptor.ssh_client_wrapper);
		}
		catch (Exception e) {
			ErrorHandling.exceptionError(e, "couldn't kill remote Chord process");
		}
	}

	@Override
	public String getApplicationName() {

		return "Chord";
	}
}
