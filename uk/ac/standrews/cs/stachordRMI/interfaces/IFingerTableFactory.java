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
/*
 * Created on 07-Jan-2005
 */
package uk.ac.standrews.cs.stachordRMI.interfaces;

/**
 * Interface defining finger table factories.
 */
public interface IFingerTableFactory {

	/**
	 * Makes a finger table for the given Chord node.
	 * 
	 * @param node a Chord node
	 * @return a new finger table for the node
	 */
	IFingerTable makeFingerTable(IChordNode node);
}
