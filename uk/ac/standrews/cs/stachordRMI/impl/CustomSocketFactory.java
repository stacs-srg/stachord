package uk.ac.standrews.cs.stachordRMI.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;


public class CustomSocketFactory implements RMIServerSocketFactory {

	private ServerSocket theSocket = null;
	private InetAddress addr;
	
	public CustomSocketFactory(InetAddress addr) throws IOException{
		this.addr = addr;
	}
	
	public ServerSocket createServerSocket(int port) throws IOException {
		InetSocketAddress sa = new InetSocketAddress(addr,port);
		theSocket = new ServerSocket();
		theSocket.bind(sa);
		return theSocket;
	}
	
	public ServerSocket getServerSocket(){
		return theSocket;
	}

}
