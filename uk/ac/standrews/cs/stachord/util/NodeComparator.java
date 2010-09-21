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
package uk.ac.standrews.cs.stachord.util;

import java.io.Serializable;
import java.util.Comparator;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class NodeComparator implements Comparator<HostDescriptor>, Serializable {

	private static final long serialVersionUID = -5679876714458357570L;

	public int compare(HostDescriptor o1, HostDescriptor o2) {
		
		if (o1 == null) return 1;
		if (o2 == null) return -1;

		IChordRemoteReference application_reference1 = (IChordRemoteReference) o1.application_reference;
		IChordRemoteReference application_reference2 = (IChordRemoteReference) o2.application_reference;
		
		return application_reference1.getKey().compareTo(application_reference2.getKey());
	}
}
