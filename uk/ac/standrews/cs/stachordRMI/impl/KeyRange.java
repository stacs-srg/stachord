/*
 *  StAChord Library
 *  Copyright (C) 2004-2008 Distributed Systems Architecture Research Group
 *  http://asa.cs.st-andrews.ac.uk/
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.stachordRMI.impl;

import java.math.BigInteger;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;

/**
 * An inclusive range of key values.
 *
 * @author stuart
 */
public class KeyRange {

	private final IKey lower_bound;
	private final IKey upper_bound;

	/**
	 * Creates a new key range.
	 * 
	 * @param lower_bound the lower bound
	 * @param upper_bound the upper bound
	 */
	public KeyRange(IKey lower_bound, IKey upper_bound) {

		this(lower_bound, upper_bound, true);
	}

	/**
	 * Creates a new key range with the specified lower bound optionally included.
	 * 
	 * @param lower_bound the lower bound
	 * @param upper_bound the upper bound
	 * @param include_lower_bound true if the specified lower bound should be included in the range
	 */
	public KeyRange(IKey lower_bound, IKey upper_bound, boolean include_lower_bound) {

		this.lower_bound = include_lower_bound ? lower_bound : new Key(lower_bound.keyValue().add(BigInteger.ONE));
		this.upper_bound = upper_bound;
	}

	/**
	 * @return the lower bound
	 */
	public IKey getLowerBound() {
		return lower_bound;
	}

	/**
	 * @return the upper bound
	 */
	public IKey getUpperBound() {
		return upper_bound;
	}
}
