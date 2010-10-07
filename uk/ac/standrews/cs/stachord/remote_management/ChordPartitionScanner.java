package uk.ac.standrews.cs.stachord.remote_management;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.remote_management.HostDescriptor;
import uk.ac.standrews.cs.nds.remote_management.HostState;
import uk.ac.standrews.cs.nds.remote_management.IGlobalHostScanner;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

class ChordPartitionScanner implements IGlobalHostScanner {
	
	@Override
	public int getMinCycleTime() {
		return 10000;
	}

	@Override
	public void check(List<HostDescriptor> host_descriptors) {
		
		// It's possible for the size of the host list, or the entries within it, to change during this method.
		// This shouldn't matter - the worst that can happen is that a node is joined to a ring it's already in,
		// which will have no effect.
		
		// Gather the running nodes that see complete cycles (non-zero recorded cycle length).
		// If any running nodes are not stable in this way then give up.
		List<HostDescriptor> stable_hosts = new ArrayList<HostDescriptor>();
		for (HostDescriptor host_descriptor : host_descriptors) {
			
			if (host_descriptor.host_state == HostState.RUNNING) {
				
				if (ringSize(host_descriptor) > 0) stable_hosts.add(host_descriptor);
				else return;
			}
		}
		
		// For each stable node with a cycle length less than the number of stable nodes, join it to the first node.
		if (stable_hosts.size() > 1) {
			
			IChordRemoteReference first_node = (IChordRemoteReference) stable_hosts.get(0).application_reference;
			
			System.out.println("starting join phase");
			for (int i = 1; i < stable_hosts.size(); i++) {
				HostDescriptor host_descriptor = stable_hosts.get(i);
				IChordRemote node = ((IChordRemoteReference) host_descriptor.application_reference).getRemote();
				try {
					if (ringSize(host_descriptor) < stable_hosts.size()) {
						System.out.println("joining " + node.getAddress() + " to " + first_node.getAddress());
						
						node.join(first_node);
					}
				}
				catch (RemoteException e) {
					Diagnostic.trace(DiagnosticLevel.FULL, "error joining rings");
				}
			}
		}
	}

	private int ringSize(HostDescriptor host_descriptor) {
		
		String ring_size_record = host_descriptor.scan_results.get(ChordManager.RING_SIZE_NAME);
		return (ring_size_record != null && !ring_size_record.equals("-")) ? Integer.parseInt(ring_size_record) : 0;
	}
}