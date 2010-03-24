package uk.ac.standrews.cs.stachordRMI.factories;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

public class CustomSocketFactory implements RMIServerSocketFactory {

	private ServerSocket server_socket;
	private InetAddress address;
	
	public CustomSocketFactory(InetAddress address) {
		this.address = address;
	} 
	
	public ServerSocket createServerSocket(int port) throws IOException {
		
		server_socket = new ServerSocket();
		server_socket.bind(new InetSocketAddress(address, port));
		
		return server_socket;
	}
	
	public ServerSocket getServerSocket(){
		return server_socket;
	}
}
