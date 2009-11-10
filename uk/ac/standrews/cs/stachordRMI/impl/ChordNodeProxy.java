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

import uk.ac.standrews.cs.nds.p2p.exceptions.P2PNodeException;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Pair;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

/**
 * Implementation of Chord node.
 * 
 * @author sja7, stuart, al, graham
 */
public class ChordNodeProxy implements IChordRemote, Remote  {
	
	// Private State
	private ChordNodeImpl cni;
	
	public ChordNodeProxy( ChordNodeImpl cni ) {
		this.cni = cni;
	}
	
	// IChordRemote Methods
	
	public InetSocketAddress getAddress() throws RemoteException {
		return cni.getAddress();
	}

	public IKey getKey() throws RemoteException  {
		return cni.getKey();
	}
	
	public void notify(IChordRemoteReference potential_predecessor) throws RemoteException {
		cni.notify(potential_predecessor);
	}

	public ArrayList<IChordRemoteReference> getSuccessorList() throws RemoteException {
		return cni.getSuccessorList();
	}
	
	public ArrayList<IChordRemoteReference> getFingerList() throws RemoteException {
		return cni.getFingerList();
	}
	
	public IChordRemoteReference getPredecessor() throws RemoteException {
		return cni.getPredecessor();
	}
	
	public IChordRemoteReference getSuccessor() throws RemoteException {
		return cni.getSuccessor();
	}

	public void isAlive() throws RemoteException {
		cni.isAlive();
	}

	public Pair<NextHopResultStatus, IChordRemoteReference> nextHop(IKey k) throws RemoteException  {
		return cni.nextHop(k);
	}
	
	public IChordRemoteReference lookup(IKey k) throws RemoteException {
		return cni.lookup(k);
	}
}


