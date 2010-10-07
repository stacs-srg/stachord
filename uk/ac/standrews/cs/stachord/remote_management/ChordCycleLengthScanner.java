/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of stachord, an independent implementation of         *
 * the Chord protocol (http://pdos.csail.mit.edu/chord/).                  *
 *                                                                         *
 * stachord is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * stachord is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with stachord.  If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                         *
 ***************************************************************************/

package uk.ac.standrews.cs.stachord.remote_management;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.ISingleHostScanner;
import uk.ac.standrews.cs.stachord.test.recovery.RecoveryTestLogic;

class ChordCycleLengthScanner implements ISingleHostScanner {
	
	@Override
	public int getMinCycleTime() {
		return 10000;
	}

	@Override
	public String getAttributeName() {
		return ChordManager.RING_SIZE_NAME;
	}

	@Override
	public void check(HostDescriptor host_descriptor) {
		
		int cycle_length = RecoveryTestLogic.cycleLengthFrom(host_descriptor, true);
		host_descriptor.scan_results.put(ChordManager.RING_SIZE_NAME, cycle_length > 0 ? String.valueOf(cycle_length) : "-");
	}

	@Override
	public String getToggleLabel() {
		// No toggle in user interface required.
		return null;
	}

	@Override
	public void setEnabled(boolean enabled) {
		// Ignore - this scanner is always enabled.
	}
}