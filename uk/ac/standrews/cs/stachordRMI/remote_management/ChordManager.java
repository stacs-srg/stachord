package uk.ac.standrews.cs.stachordRMI.remote_management;

import java.net.InetSocketAddress;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.util.ActionWithNoResult;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.nds.util.Timeout;
import uk.ac.standrews.cs.remote_management.server.IApplicationManager;
import uk.ac.standrews.cs.remote_management.server.MachineDescriptor;
import uk.ac.standrews.cs.stachordRMI.impl.ChordNodeImpl;
import uk.ac.standrews.cs.stachordRMI.test.factory.MultipleMachineNetwork;

public class ChordManager implements IApplicationManager {

	private static final int DEFAULT_RMI_REGISTRY_PORT = 1099;     // The default RMI registry port.
	private static final int APPLICATION_CALL_TIMEOUT = 10000;     // The timeout for attempted application calls, in ms.
	
	public ChordManager() {
	}

	@Override
	public void attemptApplicationCall(MachineDescriptor machine_descriptor) throws Exception {
		
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
	public void deployApplication(MachineDescriptor machine_descriptor) throws Exception {

		System.out.println("deploying to: " + machine_descriptor.host);
		MultipleMachineNetwork.createFirstNode(machine_descriptor, DEFAULT_RMI_REGISTRY_PORT);
	}

	@Override
	public void killApplication(MachineDescriptor machine_descriptor) {

		if (machine_descriptor.process != null) machine_descriptor.process.destroy();
	}
}
