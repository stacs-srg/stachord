package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.util.SortedSet;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

public interface INetwork {

	SortedSet<IChordRemoteReference> getNodes();
	
	void killNode(IChordRemoteReference node);
	
	void killAllNodes();
}