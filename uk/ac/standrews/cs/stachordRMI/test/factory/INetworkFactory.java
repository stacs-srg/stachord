package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;

public interface INetworkFactory {

	INetwork makeNetwork(int number_of_nodes) throws P2PNodeException, IOException;
}