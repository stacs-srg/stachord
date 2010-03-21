package uk.ac.standrews.cs.stachordRMI.impl;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

public class NextHopResult {

	private boolean hop_is_final;
	private IChordRemoteReference next_hop;

	public NextHopResult(boolean hop_is_final, IChordRemoteReference next_hop) {

		this.hop_is_final = hop_is_final;
		this.next_hop = next_hop;
	}

	public boolean hopIsFinal() {
		return hop_is_final;
	}

	public IChordRemoteReference nextHop() {
		return next_hop;
	}
}
