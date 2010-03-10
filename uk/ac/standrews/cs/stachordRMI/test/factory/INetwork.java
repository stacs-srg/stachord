package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.util.SortedSet;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;

public interface INetwork {

	SortedSet<IChordRemote> getNodes();
	
	void killNode( IChordRemote node );
	
	void killAllNodes();
}