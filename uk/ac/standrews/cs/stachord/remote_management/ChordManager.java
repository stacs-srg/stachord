package uk.ac.standrews.cs.stachord.remote_management;

import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.IApplicationManager;
import uk.ac.standrews.cs.nds.remote_management.ProcessInvocation;
import uk.ac.standrews.cs.nds.util.ActionWithNoResult;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.Timeout;
import uk.ac.standrews.cs.stachord.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachord.servers.StartRing;
import uk.ac.standrews.cs.stachord.test.factory.MultipleMachineNetwork;

public class ChordManager implements IApplicationManager {

	private static final String CHORD_APPLICATION_CLASSNAME = StartRing.class.getCanonicalName();
	private static final int DEFAULT_RMI_REGISTRY_PORT = 1099;     // The default RMI registry port.
	private static final int APPLICATION_CALL_TIMEOUT = 10000;     // The timeout for attempted application calls, in ms.
	
	public ChordManager() {
	}

	@Override
	public void attemptApplicationCall(HostDescriptor machine_descriptor) throws Exception {
		
		// Try to connect to the application on the default RMI port.
		final InetSocketAddress inet_socket_address = NetworkUtil.getInetSocketAddress(machine_descriptor.host, DEFAULT_RMI_REGISTRY_PORT);
		
		// Wrap the exception variable so that it can be updated by the timeout thread.
		final Exception[] exception_wrapper = new Exception[] {null};
		
		// Try to connect to the application, subject to a timeout.
		new Timeout().performActionWithTimeout(new ActionWithNoResult() {

			@Override
			public void performAction() {
				try {
					// Try to access the application at the specified address.
					ChordNodeImpl.bindToNode(inet_socket_address);
				}
				catch (Exception e) {
					// We have to store the exception here for later access, rather than throwing it, since an ActionWithNoResult can't throw exceptions and anyway
					// it's being executed in the timeout thread.
					exception_wrapper[0] = e;
				}
			}
			
		}, APPLICATION_CALL_TIMEOUT);
		
		// This is a bit horrible, but not sure how to tidy it while retaining the desired method signature.
		// The exception will be null if the application call succeeded.
		Exception e = exception_wrapper[0];
		if (e instanceof AccessException)   throw e;
		if (e instanceof RemoteException)   throw e;
		if (e instanceof NotBoundException) throw e;
	}

	@Override
	public void deployApplication(HostDescriptor machine_descriptor) throws Exception {

		Diagnostic.trace(DiagnosticLevel.RUN, "deploying to: " + machine_descriptor.host);
		
		MultipleMachineNetwork.createFirstNode(machine_descriptor, DEFAULT_RMI_REGISTRY_PORT);
	}

	@Override
	public void killApplication(HostDescriptor machine_descriptor) {
		
		// If a process handle is available, use that since it will definitely kill only the original process.
		if (machine_descriptor.process != null) {
			System.out.println("process not null");
			machine_descriptor.process.destroy();
		}
		else {
			// Otherwise, try to kill all StartRing processes.
			try {
				System.out.println("trying to kill processes matching: " + CHORD_APPLICATION_CLASSNAME);
				ProcessInvocation.killProcesses(CHORD_APPLICATION_CLASSNAME, machine_descriptor.ssh_client_wrapper);
			}
			catch (Exception e) {
				System.out.println("error trying to kill processes: " + e.getMessage());
				ErrorHandling.exceptionError(e, "couldn't kill remote Chord process");
			}
		}
	}

	@Override
	public String getApplicationName() {

		return "Chord";
	}
}
