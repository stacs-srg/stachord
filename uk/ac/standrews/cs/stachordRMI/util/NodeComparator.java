package uk.ac.standrews.cs.stachordRMI.util;

import java.util.Comparator;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordNode;

public class NodeComparator implements Comparator<IChordNode>  {

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */

	public int compare(IChordNode o1, IChordNode o2) {
		if (o1 == null) return 1;
		else if (o2==null) return -1;

		return o1.getKey().compareTo(o2.getKey());
	}

}
