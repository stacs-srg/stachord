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
package uk.ac.standrews.cs.stachordRMI.test.factory;

import uk.ac.standrews.cs.nds.util.ClassPath;
import uk.ac.standrews.cs.nds.util.SSH2ConnectionWrapper;

public class NodeDescriptor {

	public final SSH2ConnectionWrapper ssh_client_wrapper;
	public final String java_version;
	public final ClassPath class_path;
	
	public NodeDescriptor(SSH2ConnectionWrapper ssh_client_wrapper, String java_version, ClassPath class_path) {

		this.ssh_client_wrapper = ssh_client_wrapper;
		this.java_version = java_version;
		this.class_path = class_path;
	}
}
