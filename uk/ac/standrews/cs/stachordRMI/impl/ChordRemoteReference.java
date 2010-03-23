package uk.ac.standrews.cs.stachordRMI.impl;

import java.io.Serializable;
import java.rmi.RemoteException;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

/**
 * A class to hold cached keys and remote references
 *
 * @author al
 */
public class ChordRemoteReference implements IChordRemoteReference, Serializable {

	private static final long serialVersionUID = -7911452718429786447L;
	
	private IKey key;
	private IChordRemote reference;
	
	public ChordRemoteReference(IKey key, IChordRemote reference) {
		this.key = key;
		this.reference = reference;
	}

	public IKey getKey() {
		return key;
	}

	public IChordRemote getRemote() {
		return reference;
	}
	
	public boolean equals( Object o ) {
		return o instanceof ChordRemoteReference && key.equals( ((ChordRemoteReference) o).getKey() );
	}
	
	public String toString() {
		String ref;
		try {
			ref = getRemote().getAddress().toString();
		} catch (RemoteException e) {
			ref = "--IP down--";
		}
		return "ChordRemoteReference to: " + key + " " + ref;
	}
}
