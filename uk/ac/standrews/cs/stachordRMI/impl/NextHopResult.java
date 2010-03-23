package uk.ac.standrews.cs.stachordRMI.impl;

import java.io.Serializable;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

public class NextHopResult implements Serializable {

	private static final long serialVersionUID = 2162948760764524096L;
	
	private boolean hop_is_final;
	private IChordRemoteReference hop;

	public NextHopResult(boolean hop_is_final, IChordRemoteReference hop) {

		this.hop_is_final = hop_is_final;
		this.hop = hop;
	}

	public boolean hopIsFinal() {
		return hop_is_final;
	}

	public IChordRemoteReference getHop() {
		return hop;
	}
}
