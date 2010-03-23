package uk.ac.standrews.cs.stachordRMI.impl;

import java.io.Serializable;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

public class NextHopResult implements Serializable {

	private static final long serialVersionUID = 2162948760764524096L;
	
	private boolean is_final_hop;
	private IChordRemoteReference hop;

	public NextHopResult(boolean is_final_hop, IChordRemoteReference node) {

		this.is_final_hop = is_final_hop;
		this.hop = node;
	}

	public boolean isFinalHop() {
		return is_final_hop;
	}

	public IChordRemoteReference getNode() {
		return hop;
	}
}
