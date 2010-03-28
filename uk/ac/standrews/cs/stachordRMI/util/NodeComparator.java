package uk.ac.standrews.cs.stachordRMI.util;

import java.io.Serializable;
import java.util.Comparator;

import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemoteReference;

public class NodeComparator implements Comparator<IChordRemoteReference>, Serializable {

	private static final long serialVersionUID = -5679876714458357570L;

	public int compare(IChordRemoteReference o1, IChordRemoteReference o2) {
		
		if (o1 == null) return 1;
		if (o2 == null) return -1;

		return o1.getKey().compareTo(o2.getKey());
	}
}
