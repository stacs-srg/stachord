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
package uk.ac.standrews.cs.stachord.interfaces;

import java.io.Serializable;
import java.net.InetSocketAddress;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

/**
 * Interface to hold a remote reference to a Chord node, with a cached key and address.
 *
 * @author al
 */
public interface IChordRemoteReference extends Serializable {

	/**
	 * @return the key associated with this reference
	 */
	IKey getKey();
	
	/**
	 * @return the address associated with this reference
	 */
	InetSocketAddress getAddress();
	
	/**
	 * @return the remote reference
	 */
	IChordRemote getRemote();
}
