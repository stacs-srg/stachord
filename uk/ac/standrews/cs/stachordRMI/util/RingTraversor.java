/*
 *  ASA Library
 *  Copyright (C) 2004-2010 Distributed Systems Architecture Research Group
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
package uk.ac.standrews.cs.stachordRMI.util;

import java.rmi.registry.LocateRegistry;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.servers.ChordServer;

/**
 * <p>Traverses a Chord ring to check that it is closed.
 * 
 * <p>The calling class must provide a hostname and port of a known node in the ring, which is used to
 * obtain a reference to the node, and repeatedly call successors until the known node is found again.
 * 
 * <p>In its current form the process will loop until either the known node is found again (meaning the ring is closed),
 * or until an exception is thrown.
 * 
 * <p>This class can also be run periodically by running it as a seperate thread (the run method is implemented). A default
 * interval will be used unless another is specifed in the constructor call to create the class.
 * 
 * @author Angus Macdonald (angus@cs.st-andrews.ac.uk), based on code by Markus Tauber
 */

public class RingTraversor extends Thread {

	private final boolean traverse = true;
	
	private IChordRemote startNode;

	public int traversal_interval =  10000;
	
	/**
	 * Connect to a known node at the specified location, using a default interval if the traversal is to
	 * be run multiple times as a seperate thread.
	 * 
	 * @param hostname	Host on which the known node is running.
	 * @param port		Port on which the known node is running.
	 */
	public RingTraversor(String hostname, int port) {
		
		try {
			this.startNode = (IChordRemote) LocateRegistry.getRegistry( hostname, port ).lookup( ChordServer.CHORD_REMOTE_SERVICE );
		} catch (Exception e) {
			ErrorHandling.hardError("Failed to find the known node in this chord ring [registry at: " + hostname + ":" + port + "]");
		}
		
	}
	
	/**
	 * Connect to a known node at the specified location, using the specifed interval if the traversal to be run multiple times as a
	 * seperate thread.
	 * 
	 * @param hostname	Host on which the known node is running.
	 * @param port		Port on which the known node is running.
	 * @param interval	Timing interval between checking whether the ring is closed.
	 */
	public RingTraversor(String hostname, int port, int interval) {
		this(hostname, port);
		
		this.traversal_interval = interval;
	}


	@Override
	public void run() {
		
		Diagnostic.traceNoEvent(DiagnosticLevel.FINAL, "ChordRingTraversor running. Checking ring every " + traversal_interval / 1000 + " seconds.");
		
		try {
			
			while (traverse) {

				traverseJChordRing(startNode);
				
				try {
					sleep(traversal_interval);
				} catch (InterruptedException e) {
					ErrorHandling.exceptionError(e, getName() + " interrupted at " + System.currentTimeMillis());
				}
			}
		} catch (Exception e) {
			ErrorHandling.error("Caught an exception that will cause this ring traverser thread to terminate");
		}
	}

	
	// from TestUtils.traverseJChordRing(startNode, nodes.size());
	//
	public static boolean traverseJChordRing(IChordRemote startNode) {

		int count = 0;

		try {
			count = 0;
			IChordRemote current = startNode;

			do {
				Diagnostic.traceNoEvent(DiagnosticLevel.FULL, current.getAddress().getHostName() + ":" + (current.getAddress().getPort() ) + 
						" Key:" + current.getKey() + " Pred: " + current.getPredecessor().getKey() + " Succ: " + current.getSuccessor().getKey() );
				

				if (current.getSuccessor() == null){
					Diagnostic.trace("Node at port " + current.getAddress().getPort() + " does not have a successor.");
					throw new Exception("Node at port " + current.getAddress().getPort() + " does not have a successor.");
				}
				
				IChordRemote succ = current.getSuccessor().getRemote();
				
				if (current.getPredecessor() == null){
					Diagnostic.trace("Node at port " + current.getAddress().getPort() + " does not have a predecessor.");
					throw new Exception("Node at port " + current.getAddress().getPort() + " does not have a predecessor.");
				}
				
				IChordRemote pred = current.getPredecessor().getRemote();
				IKey current_key = current.getKey();
				
				
				if (succ != null && !succ.getPredecessor().getKey().equals(current_key)){
					Diagnostic.trace("Predecessor of " + current_key + " doesn't match [" + succ.getPredecessor().getKey() + "]");
				}
				if (pred != null && !pred.getSuccessor().getKey().equals(current_key)){
					Diagnostic.trace("Predecessor of " + current_key + " doesn't match [" + succ.getPredecessor().getKey() + "]");
				}
				
				current = succ;
				
				count++;
			} while (current != null && !current.getKey().equals(startNode.getKey()));
				
			Diagnostic.trace("Ring traversor event " + "," + System.currentTimeMillis() + "," + count
					+ ",RING_CLOSED");
			return true;
		} catch (Exception e) {
			Diagnostic.trace("Ring traversor event " + "," + System.currentTimeMillis() + "," + count
					+ ",RING_OPEN");
			return false;
		}
	}

	public static void main(String[] args) {
		Diagnostic.setLevel(DiagnosticLevel.FULL);
		
		if (args.length == 2) { //Just hostname and port specified, use default interval.
			
			String hostname = args[0];
			int port = Integer.parseInt(args[1]);
			
			RingTraversor traverser = new RingTraversor(hostname, port);
			
			traverser.start();
		} else if (args.length == 3) { //Hostname, port and interval specified. 
			String hostname = args[0];
			int port = Integer.parseInt(args[1]);
			
			int interval = Integer.parseInt(args[2]);

			RingTraversor traverser = new RingTraversor(hostname, port, interval);
			
			traverser.start();
			
		} else {
			usage();
		}
	}


	public static void usage() {

		System.out.println("Iterates through a Chord ring starting from gateway to check if the ring is closed.\n options:");

		System.out.println("\t hostname port (e.g.: 192.168.1.1 2000) ");
		System.out.println("\t hostname port interval[ms] (e.g.: 192.168.1.1 23555 10000 ) ");
	}

}
