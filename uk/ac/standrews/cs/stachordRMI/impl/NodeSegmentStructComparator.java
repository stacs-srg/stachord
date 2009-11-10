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
/*
 * Created on 11-Feb-2005
 */
package uk.ac.standrews.cs.stachordRMI.impl;

import java.rmi.RemoteException;
import java.util.Comparator;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;


/**
 * @author stuart
 */
public class NodeSegmentStructComparator implements Comparator<NodeSegmentStruct> {

	private final IChordNode localNode;

	public NodeSegmentStructComparator(IChordNode localNode){
		this.localNode=localNode;
	}

	public int compare(NodeSegmentStruct kss0, NodeSegmentStruct kss1) {

		if(kss0==null)return -1;
		if(kss1==null)return 1;

		IKey k0 = kss0.fingerKey;
		IKey k1 = kss1.fingerKey;
		if(kss0.equals(kss1))
			return 0;
		else {
				if(localNode.getKey().ringDistanceTo(k0).compareTo(localNode.getKey().ringDistanceTo(k1))<0)
					return 1;
				else
					return -1;
		}

	}

}
