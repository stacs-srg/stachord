package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.rmi.RemoteException;
import java.util.SortedSet;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;

public interface INodeFactory {

	public SortedSet<IChordRemote> makeNetwork(int number_of_nodes) throws RemoteException, P2PNodeException;
	
	public void deleteNode( IChordRemote node );
}