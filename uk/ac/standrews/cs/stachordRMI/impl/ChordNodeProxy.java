/*
 *  StAChord Library
 *  Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 *  http://asa.cs.st-andrews.ac.uk/
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.stachordRMI.impl;

import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

public class ChordNodeProxy implements IChordRemote, Remote  {
	
	// Private State
	private ChordNodeImpl cni;
	private boolean node_failed = false; // used in simulation to make node inaccessible
	
	public ChordNodeProxy( ChordNodeImpl cni ) {
		this.cni = cni;
	}
	
	// IChordRemote Methods
	
	public InetSocketAddress getAddress() throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		return cni.getAddress();
	}

	public IKey getKey() throws RemoteException  {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		return cni.getKey();
	}
	
	public void notify(IChordRemoteReference potential_predecessor) throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		cni.notify(potential_predecessor);
	}

	public ArrayList<IChordRemoteReference> getSuccessorList() throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		return cni.getSuccessorList();
	}
	
	public ArrayList<IChordRemoteReference> getFingerList() throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		return cni.getFingerList();
	}
	
	public IChordRemoteReference getPredecessor() throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		return cni.getPredecessor();
	}
	
	public IChordRemoteReference getSuccessor() throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		return cni.getSuccessor();
	}

	public void isAlive() throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		cni.isAlive();
	}

	public NextHopResult nextHop(IKey k) throws RemoteException  {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		return cni.nextHop(k);
	}
	
	public IChordRemoteReference lookup(IKey k) throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		return cni.lookup(k);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cni == null) ? 0 : cni.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof ChordNodeProxy) {
			
			ChordNodeProxy other = (ChordNodeProxy) obj;
			
			return (cni == null && other.cni == null) ||
			       (cni != null && other.cni != null && cni.equals(other.cni));
		}
		else return false;
	}

	/* 
	 * Stops the proxy from accessing the ChordNodeImpl
	 */
	public void destroy() {
		node_failed = true;
	}

	public void enableFingerTableMaintenance(boolean enabled) throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		cni.enableFingerTableMaintenance(enabled);
	}

	public void fingerFailure(IChordRemoteReference broken_finger) throws RemoteException {
		if( node_failed ) throw new RemoteException(); // to simulate failure
		cni.fingerFailure(broken_finger);
	}	
}
