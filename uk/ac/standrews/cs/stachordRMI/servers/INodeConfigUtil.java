package uk.ac.standrews.cs.stachordRMI.servers;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.eventModel.eventBus.busInterfaces.IEventBus;
import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.interfaces.IApplicationRegistry;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.util.HashBasedKeyFactory;
import uk.ac.standrews.cs.stachordRMI.deploy.DefaultMaintenanceThread;
import uk.ac.standrews.cs.stachordRMI.deploy.MaintenanceThread;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;

public interface INodeConfigUtil {

	public IChordNode getProtocolNode();

	public void processCLA(String[] args, int max_number_of_args,
			Class<?> process) throws P2PNodeException;

	public void processCLA(String[] args, int max_number_of_args,
			Class<?> process, String message) throws P2PNodeException;

	public void processCLA(String[] args, int max_number_of_args,
			int min_number_of_args, Class<?> process, String message)
			throws P2PNodeException;

	/**
	 * Deploy the node by calling initialiseP2PNode and passing the processed
	 * CLA state as arguments. This method can only be called if processCLA has
	 * been called.
	 * 
	 * @throws P2PNodeException
	 */
	public void startNode() throws P2PNodeException;

	public void initialiseP2PNode(String address, int localPort,
			String knownNodeAddress, int knownNodePort, IKey localKey,
			IEventBus bus) throws P2PNodeException;

	public HashBasedKeyFactory getKeyFactory();

	public int getDefaultPort();

	public IApplicationRegistry getApplicationRegistry();

	public IChordNode getLocal_chord_node();

	public InetSocketAddress getLocalAddress();

	public IKey getLocalKey();

	public IEventBus getBus();

	public void setBus(IEventBus bus);

	public void setApplicationRegistry(
			IApplicationRegistry applicationRegistry);

	public HashBasedKeyFactory getKey_factory();

	public DefaultMaintenanceThread getNodeThread();
	
	public void setMaintenanceThreadClass(Class<? extends MaintenanceThread> t);
}