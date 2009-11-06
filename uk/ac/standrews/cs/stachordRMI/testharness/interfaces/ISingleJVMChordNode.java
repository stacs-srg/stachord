package uk.ac.standrews.cs.stachordRMI.testharness.interfaces;

import uk.ac.standrews.cs.nds.eventModel.eventBus.busInterfaces.IEventBus;
import uk.ac.standrews.cs.stachordRMI.deploy.MaintenanceThread;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;

public interface ISingleJVMChordNode extends Comparable<ISingleJVMChordNode> {

	public IChordNode getNode();
	public MaintenanceThread getNodeThread();
	public IEventBus getNodeEventBus();
}
