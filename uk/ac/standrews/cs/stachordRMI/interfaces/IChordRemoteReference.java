package uk.ac.standrews.cs.stachordRMI.interfaces;

import java.io.Serializable;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

/**
 * An interface to hold a cached Key and a remote reference
 *
 * @author al
 */
public interface IChordRemoteReference extends Serializable {

	/**
	 * @return the key associated with this reference
	 */
	IKey getKey();
	
	/**
	 * @return the remote reference
	 */
	IChordRemote getRemote();
}
