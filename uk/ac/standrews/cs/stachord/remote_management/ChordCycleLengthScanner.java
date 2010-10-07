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
}