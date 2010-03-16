package uk.ac.standrews.cs.stachordRMI.test.factory;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public interface INetworkFactory {

	INetwork makeNetwork(int number_of_nodes, String network_type) throws RemoteException, IOException, NotBoundException;
}