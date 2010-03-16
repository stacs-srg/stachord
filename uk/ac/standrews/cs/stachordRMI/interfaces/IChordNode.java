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
package uk.ac.standrews.cs.stachordRMI.interfaces;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

/**
 * Defines locally accessible Chord node functionality.
 *
 * @author graham
 */
public interface IChordNode {
	
	public static final String CHORD_REMOTE_SERVICE = IChordNode.class.getSimpleName();
	
	IChordRemoteReference lookup( IKey key ) throws RemoteException;
	
	InetSocketAddress getAddress();

	IKey getKey();

	IChordRemoteReference getSuccessor();
	
	IChordRemoteReference getPredecessor();
	
	IChordRemoteReference getProxy();
}
