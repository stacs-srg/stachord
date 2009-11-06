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
package uk.ac.standrews.cs.stachordRMI.deploy;

import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;

public class DiagnosticMaintenanceThread extends MaintenanceThread {

	private static final String SELF_REPAIR_EXECUTION_EVENT = "SELF_REPAIR_EXECUTION_EVENT";

	public static final int DEFAULT_WAIT_PERIOD = 1000; // 1 second.

	
	private int sequenceNbr = 0;

	public DiagnosticMaintenanceThread(IChordNode node) {
		super(node);
		setName("Diagnostic Chord Maintenance Protocol Thread");
	}

	@Override
	public void run() {
		String nodeStr = node.getAddress() + "(" + node.getKey() + ")";
		try {

			while (running) {
				try {
					sleep(DEFAULT_WAIT_PERIOD);
				} catch (InterruptedException e) {
					// do nothing
				}

				sequenceNbr++;

				DiagnosticLevel diag_level = DiagnosticLevel.FINAL;

				Diagnostic.trace(diag_level, SELF_REPAIR_EXECUTION_EVENT, ",",
						System.currentTimeMillis(), ",CHECK_PREDECESSOR", ",",
						sequenceNbr, ",INVOKED");
				node.checkPredecessor();
				Diagnostic.trace(diag_level, SELF_REPAIR_EXECUTION_EVENT, ",",
						System.currentTimeMillis(), ",CHECK_PREDECESSOR", ",",
						sequenceNbr, ",FINISHED");

				/* OutOfMemory Exception happens in here */
				Diagnostic.trace(diag_level, SELF_REPAIR_EXECUTION_EVENT, ",",
						System.currentTimeMillis(), ",STABILIZE", ",",
						sequenceNbr, ",INVOKED");
				node.stabilize();
				Diagnostic.trace(diag_level, SELF_REPAIR_EXECUTION_EVENT, ",",
						System.currentTimeMillis(), ",STABILIZE", ",",
						sequenceNbr, ",FINISHED");

				Diagnostic.trace(diag_level, SELF_REPAIR_EXECUTION_EVENT, ",",
						System.currentTimeMillis(), ",FIX_NEXT_FINGER", ",",
						sequenceNbr, ",INVOKED");
				node.fixNextFinger();
				Diagnostic.trace(diag_level, SELF_REPAIR_EXECUTION_EVENT, ",",
						System.currentTimeMillis(), ",FIX_NEXT_FINGER", ",",
						sequenceNbr, ",FINISHED");

			}
			System.err.println("maintenance thread stopping on node" + nodeStr);
		} catch (final OutOfMemoryError e) {
			ErrorHandling
					.exceptionError(
							e,
							"Caught a OutOfMemoryError exception. This will cause this house-keeping thread to terminate.\n\t",
							"node key: ", node.getKey());
		} catch (Exception e) {
			ErrorHandling.exceptionError(e,
					"maintenance thread exception on node" + nodeStr);
		}
	}
}
