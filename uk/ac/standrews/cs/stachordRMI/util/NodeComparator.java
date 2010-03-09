package uk.ac.standrews.cs.stachordRMI.util;

import java.rmi.RemoteException;
import java.util.Comparator;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;

public class NodeComparator implements Comparator<IChordRemote>  {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */

	public int compare(IChordRemote o1, IChordRemote o2) {
		if (o1 == null) return 1;
		else if (o2==null) return -1;

		try {
			return o1.getKey().compareTo(o2.getKey());
		} catch (RemoteException e) {
			return -1;
		}
	}

}
