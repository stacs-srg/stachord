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
/**
 * Created on Aug 8, 2005 at 8:36:59 AM.
 */
package uk.ac.standrews.cs.stachordRMI.deploy;

/**
 * Wrapper containing a process and details of the host where it has been created.
 *
 * @author graham
 */
public class ProcessEvent {

	private Process process;
	private String host_and_port;

	public ProcessEvent(Process process, String host_and_port) {
		
		this.process = process;
		this.host_and_port = host_and_port;
	}

	public Process getProcess() {
		return process;
	}

	public String getHostAndPort() {
		return host_and_port;
	}
}
