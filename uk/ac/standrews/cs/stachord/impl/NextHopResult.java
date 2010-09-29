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

import java.io.Serializable;

import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class NextHopResult implements Serializable {

	private static final long serialVersionUID = 2162948760764524096L;
	
	private boolean is_final_hop;
	private IChordRemoteReference node;

	public NextHopResult(boolean is_final_hop, IChordRemoteReference node) {

		this.is_final_hop = is_final_hop;
		this.node = node;
	}

	public boolean isFinalHop() {
		return is_final_hop;
	}

	public IChordRemoteReference getNode() {
		return node;
	}
}
