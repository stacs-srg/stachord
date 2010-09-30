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
package uk.ac.standrews.cs.stachord.impl;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Holds a reference to a remote Chord node, with a locally cached copy of its key and IP address.
 *
 * @author Alan Dearle (al@cs.st-andrews.ac.uk)
 * @author Graham Kirby(graham@cs.st-andrews.ac.uk)
 */
class ChordRemoteReference implements IChordRemoteReference {

	private static final long serialVersionUID = -7911452718429786447L;
	
	private IKey key;
	private InetSocketAddress address;
	private IChordRemote reference;
	
	public ChordRemoteReference(IKey key, IChordRemote reference) throws RemoteException {
		
		this.key = key;
		this.reference = reference;
		
		address = reference.getAddress();
	}

	public IKey getKey() {
		return key;
	}

	public InetSocketAddress getAddress() {

		return address;
	}
	
	public IChordRemote getRemote() {
		return reference;
	}

	@Override
	public int hashCode() {

		return 31 + ((key == null) ? 0 : key.hashCode());
	}

	public boolean equals( Object o ) {
		return o instanceof ChordRemoteReference && key.equals( ((ChordRemoteReference) o).getKey() );
	}
	
	public String toString() {
		
		return getRemote().toString();
	}
}
