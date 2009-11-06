package uk.ac.standrews.cs.stachordRMI.testharness.impl;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import uk.ac.standrews.cs.nds.eventModel.eventBus.busInterfaces.IEventBus;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.impl.P2PStatus;
import uk.ac.standrews.cs.nds.p2p.interfaces.IApplicationRegistry;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.stachordRMI.deploy.DefaultMaintenanceThread;
import uk.ac.standrews.cs.stachordRMI.deploy.MaintenanceThread;
import uk.ac.standrews.cs.stachordRMI.deploy.SingleJVMChordDeployment;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.ISingleJVMChordNode;

public class SingleJVMChordNodeFactory {
	
	public static ISingleJVMChordNode makeNode(int localPort, IChordNode knownNode,
			IKey localKey, IEventBus bus, boolean startThread, IApplicationRegistry registry) throws P2PNodeException {
		
		IChordNode local_chord_node = SingleJVMChordDeployment.customDeployment(
				defaultAddress(localPort), localKey, knownNode, bus, registry);

		MaintenanceThread nodeThread = new DefaultMaintenanceThread(local_chord_node);
		if(startThread){
			nodeThread.start();
		}
		
		return new SingleJVMChordNode(local_chord_node,nodeThread,bus);
	}
	
	public static ISingleJVMChordNode makeNode(int localPort, IChordNode knownNode,
			IKey localKey, IEventBus bus, boolean startThread) throws P2PNodeException {
		
		return makeNode(localPort, knownNode, localKey, bus, startThread, null);
	}

	public static ISingleJVMChordNode makeNode(int localPort, IChordNode knownNode,
			IKey localKey, IEventBus bus) throws P2PNodeException {
		return makeNode(localPort, knownNode, localKey, bus, true);
	}
	
	private static InetSocketAddress defaultAddress(int localPort)
			throws P2PNodeException {
		InetSocketAddress configLocalAddress = null;
		try {
			configLocalAddress = NetworkUtil
					.getLocalIPv4InetSocketAddress(localPort);
		} catch (IllegalArgumentException e1) {
			throw new P2PNodeException(P2PStatus.LOCAL_PORT_INIT_FAILURE,
					"Specified port is incorrect");
		} catch (UnknownHostException e1) {
			throw new P2PNodeException(P2PStatus.LOCAL_HOST_INIT_FAILURE,
					"Cannot determine default local address");
		}
		return configLocalAddress;
	}
	
}
