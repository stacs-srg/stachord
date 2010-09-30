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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

class SuccessorList {

	private final ChordNodeImpl local_node;
	private final ArrayList<IChordRemoteReference> successor_list;     // Needs to be typed as implementation type for serialization.

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	public SuccessorList(ChordNodeImpl local_node) {

		this.local_node = local_node;
		successor_list = new ArrayList<IChordRemoteReference>();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Searches the successor list for a working node.
	 * 
	 * @return the first working node in the SuccessorList
	 * @throws NoReachableNodeException if no working node is found
	 */
	protected IChordRemoteReference findFirstWorkingNode() throws NoReachableNodeException {

		for (IChordRemoteReference next : successor_list) {
			try {
				next.getRemote().isAlive();
				return next;
			}
			catch (RemoteException e) {}
		}
		throw new NoReachableNodeException();
	}

	/**
	 * @return the successor list
	 */
	protected ArrayList<IChordRemoteReference> getList() {
		return successor_list;
	}

	/**
	 * Constructs a new successor list which consists of this node's successor
	 * followed by the first (MAX_SIZE-1) elements of the successor's successor
	 * list.
	 */
	protected void refreshList() {

		// This is a new ring or we have collapsed back to a single node.
		if (successor_list.size() > 0) {

			// The successor list is not empty.
			successor_list.clear();
		}
	}

	/**
	 * Constructs a new successor list which consists of this node's successor
	 * followed by the first (MAX_SIZE-1) elements of the successor's successor
	 * list.
	 */
	protected boolean refreshList(List<IChordRemoteReference> successor_list_of_successor) {

		IChordRemoteReference successor = local_node.getSuccessor();

		ArrayList<IChordRemoteReference> new_list = new ArrayList<IChordRemoteReference>();

		int numElements = Math.min(Constants.MAX_SUCCESSOR_LIST_SIZE - 1, successor_list_of_successor.size());

		// Check for the element of the successor list being this node, as will
		// happen with a small number of nodes in the ring. If this node is
		// found in the received successor list then that element and all
		// elements following it are discarded.

		for (int i = 0; i < numElements; i++) {

			IChordRemoteReference node = successor_list_of_successor.get(i);
			if (node.getKey().equals(local_node.getKey())) break;

			new_list.add(node);
		}

		new_list.add(0, successor);
		
		if (!new_list.equals(successor_list)) {
		
			successor_list.clear();
			successor_list.addAll(new_list);
			return true;
		}
		else return false;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String toString() {
		
		StringBuilder buffer = new StringBuilder();
		buffer.append("\n");
		
		if (successor_list.isEmpty()) {
			buffer.append("empty");
		}
		else {
			for (IChordRemoteReference successor : successor_list) {
				
				buffer.append("successor: ");
				buffer.append(successor != null ? successor.getKey() : "null");
				buffer.append(" address: " + successor.getAddress());
				buffer.append("\n");
			}
		}
		
		return buffer.toString();
	}
}
