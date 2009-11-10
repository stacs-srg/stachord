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

	private IKey key;
	private IChordRemote reference;
	
	/**
	 * Null constructor for RMI
	 * 
	 */	public ChordRemoteReference() {
		this.key = null;
		this.reference = null;
	}
	
	/**
	 * @param key - the key of a remote reference
	 * @param reference - the remote reference
	 */
	public ChordRemoteReference(IKey key, IChordRemote reference) {
		this.key = key;
		this.reference = reference;
	}

	/* (non-Javadoc)
	 * @see uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference#getKey()
	 */
	public IKey getKey() {
		return key;
	}

	/* (non-Javadoc)
	 * @see uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference#getRemote()
	 */
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
