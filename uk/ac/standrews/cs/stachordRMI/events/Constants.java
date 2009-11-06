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
package uk.ac.standrews.cs.stachordRMI.events;

public class Constants {

	// infos in the events which are sent when a maintained field is accessed
	public static final String FIELD_ACCESS_OK = "FIELD_ACCESS_OK";

	// events types sent by chord node impl when a maintained field is accessed
	/////////////////////////////////////
	public static final String FINGER_TABLE_ACCESS_EVENT = "FINGER_TABLE_ACCESS_EVENT";
	public static final String PREDECCESSOR_ACCESS_EVENT = "PREDECCESSOR_ACCESS_EVENT";
	public static final String SUCCESSOR_ACCESS_EVENT = "SUCCESSOR_ACCESS_EVENT";
	public static final String FIELD_ACCESS_FAILED = "FIELD_ACCESS_FAILED";

	// events types sent by chord node impl's self repair operations
	//////////////////////////////////////
	public static final String STABILISE_EVENT = "STABILISE_EVENT";
	public static final String FIX_NEXT_FINGER_EVENT = "FIX_NEXT_FINGER_EVENT";
	public static final String CHECK_PREDECESSOR_EVENT = "CHECK_PREDECESSOR_EVENT";
	public static final String SELF_REPAIR_STATE_CHANGE = "SELF_REPAIR_STATE_CHANGE";
	//
	public static final String FIND_SUCCESSOR_TIME_MONITORING_EVENT = "FIND_SUCCESSOR_TIME_MONITORING_EVENT";

	// infos in the events from the self repair operations
	public static final String SELF_REPAIR_INVOKED = "SELF_REPAIR_INVOKED";
	public static final String SELF_REPAIR_HAD_NO_EFFECT = "SELF_REPAIR_HAD_NO_EFFECT";
}
