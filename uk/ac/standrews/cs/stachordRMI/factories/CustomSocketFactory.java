/*******************************************************************************
 * StAChord Library
 * Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 * http://asa.cs.st-andrews.ac.uk/
 * 
 * This file is part of stachordRMI.
 * 
 * stachordRMI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * stachordRMI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with stachordRMI.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
