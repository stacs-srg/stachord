package uk.ac.standrews.cs.stachordRMI.testharness.impl;

import uk.ac.standrews.cs.nds.eventModel.eventBus.busInterfaces.IEventBus;
import uk.ac.standrews.cs.stachordRMI.deploy.MaintenanceThread;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.testharness.interfaces.ISingleJVMChordNode;

public class SingleJVMChordNode implements ISingleJVMChordNode {
	IChordNode node;
	MaintenanceThread nodeThread;
	IEventBus bus;
	
	public SingleJVMChordNode(IChordNode node, MaintenanceThread nodeThread, IEventBus bus) {
		super();
		this.node = node;
		this.nodeThread = nodeThread;
		this.bus=bus;
	}

	public IChordNode getNode() {
		return node;
	}

	public MaintenanceThread getNodeThread() {
		return nodeThread;
	}

	public IEventBus getNodeEventBus() {
		return bus;
	}

	public int compareTo(ISingleJVMChordNode o) {
		return node.getKey().compareTo(o.getNode().getKey());
	}
	
}
