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

import java.net.InetSocketAddress;
import java.util.List;

import uk.ac.standrews.cs.nds.eventModel.Event;
import uk.ac.standrews.cs.nds.eventModel.IEvent;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachordRMI.impl.KeyRange;
import uk.ac.standrews.cs.stachordRMI.interfaces.IChordRemote;

public class ChordEventFactory {
	public static final String PRED_REP_EVENT = "PredecessorRepEvent";
	public static final String PRED_REP_EVENT_PRED_ADDR = "pred";
	public static final String PRED_REP_EVENT_PRED_KEY = "pred_key";

	public static final String SUCC_REP_EVENT = "SuccessorRepEvent";
	public static final String SUCC_REP_EVENT_SUCC_ADDR = "succ";
	public static final String SUCC_REP_EVENT_SUCC_KEY = "suc_key";

	public static final String NFN_REP_EVENT = "NodeFailureNotificationRepEvent";
	public static final String NFN_REP_EVENT_FAILED_ADDR = "failed";
	public static final String NFN_REP_EVENT_FAILED_KEY = "failed_key";

	public static final String SUCC_STATE_REP_EVENT = "SuccessorStateEvent";
	public static final String SUCC_STATE_REP_EVENT_OLD = "oldSuccessor";
	public static final String SUCC_STATE_REP_EVENT_NEW = "newSuccessor";

	public static final String SUCCESSORLIST_CHANGE_EVENT = "SuccessorListChangeEvent";
	public static final String SUCCESSORLIST_CHANGE_NEW_LIST = "new_list";
	public static final String SUCCESSORLIST_CHANGE_KEY_RANGE = "key_range";
	public static final String SUCCESSORLIST_CHANGE_REMOVED = "removed";
	public static final String SUCCESSORLIST_CHANGE_ADDED = "added";

	public static final String ROUTING_CALL_EVENT = "RoutingCallEvent";
	public static final String ROUTING_CALL_BY ="routing_call_by";
	public static final String ROUTING_CALL_TIME="routing_request_start_time";

	public static final String ROUTING_CALL_FAILURE_ON_FIRST_FINGER_TABLE_EVENT = "FirstRoutingHopFailure";
	public static final String ROUTING_CALL_FAILURE_ON_FIRST_FINGER_TABLE_BY = "called_by";
	public static final String ROUTING_CALL_FAILURE_ON_FIRST_FINGER_TABLE_TIME = "failure_time";

	public static final String DOL_ROUTING_HOP_EVENT = "DOL_Hop";
	public static final String DOL_ROUTE_EVENT = "DOL_Route";
	public static final String DOL_RESOLUTION_EVENT = "DOL_Resolve";

	public static final String CHORD_ROUTING_HOP_EVENT = "RoutingHop";
	public static final String CHORD_ROUTE_EVENT = "Route";

	public static final String ROOT_NODE_EVENT = "RootNode";

	public static final String ROUTE_HOPS = "routing_hops";

	public static final String ROUTING_EVENT_SOURCE = "event_source";
	public static final String ROUTING_EVENT_DESTINATION = "event_dest";

	public static Event makeNodeFailureNotificationRepEvent(InetSocketAddress failed, IKey failed_key) {

		Event event = new Event(NFN_REP_EVENT);

		event.put(NFN_REP_EVENT_FAILED_ADDR, failed);
		event.put(NFN_REP_EVENT_FAILED_KEY, failed_key);

		return event;
	}

	public static Event makePredecessorRepEvent(InetSocketAddress pred, IKey pred_key) {

		Event event = new Event(PRED_REP_EVENT);

		event.put(PRED_REP_EVENT_PRED_ADDR, pred);
		event.put(PRED_REP_EVENT_PRED_KEY, pred_key);

		return event;
	}

	public static Event makePredecessorRepEvent(IChordRemote new_predecessor) {

		if (new_predecessor == null) return makePredecessorRepEvent(null, null);
		else                         return makePredecessorRepEvent(new_predecessor.getAddress(), new_predecessor.getKey());
	}

	public static Event makeSuccessorRepEvent(InetSocketAddress succ, IKey succ_key) {

		Event event = new Event(SUCC_REP_EVENT);

		event.put(SUCC_REP_EVENT_SUCC_ADDR, succ);
		event.put(SUCC_REP_EVENT_SUCC_KEY, succ_key);

		return event;
	}

	public static Event makeSuccessorStateEvent(IChordRemote newSuccessor, IChordRemote oldSuccessor) {

		Event event = new Event(SUCC_STATE_REP_EVENT);

		event.put(SUCC_STATE_REP_EVENT_NEW, newSuccessor);
		event.put(SUCC_STATE_REP_EVENT_OLD, oldSuccessor);

		return event;
	}

	public static Event makeSuccessorListChangeEvent(List<IChordRemote> store, KeyRange keyRange, List<IChordRemote> new_list, List<IChordRemote> added, List<IChordRemote> removed) {

		Event event = new Event(SUCCESSORLIST_CHANGE_EVENT);

		event.put(SUCCESSORLIST_CHANGE_KEY_RANGE,keyRange);
		event.put(SUCCESSORLIST_CHANGE_NEW_LIST,new_list);
		event.put(SUCCESSORLIST_CHANGE_ADDED,added);
		event.put(SUCCESSORLIST_CHANGE_REMOVED,removed);
		return event;
	}

	/**
	 * Event to monitor the number of routing calls
	 *
	 */
	public static Event makeRoutingCallEvent(String function,long time_routingcall_start) {

		Event event = new Event(ROUTING_CALL_EVENT);

		event.put(ROUTING_CALL_BY,function);
		event.put(ROUTING_CALL_TIME,time_routingcall_start);
		return event;
	}

	public static Event makeDOLRoutingHopEvent(String source, String dest) {
		Event event = new Event(DOL_ROUTING_HOP_EVENT);
		event.put(ROUTING_EVENT_SOURCE, source);
		event.put(ROUTING_EVENT_DESTINATION, dest);
		return event;
	}

	public static IEvent makeChordRoutingHopEvent(String source, String hop) {
		IEvent event = new Event(CHORD_ROUTING_HOP_EVENT);
		event.put(ROUTING_EVENT_SOURCE, source);
		event.put(ROUTING_EVENT_DESTINATION, hop);
		return event;
	}

	public static IEvent makeChordRootNodeEvent(String source, String root) {
		IEvent event = new Event(ROOT_NODE_EVENT);

		event.put(ROUTING_EVENT_SOURCE, source);
		event.put(ROUTING_EVENT_DESTINATION, root);
		return event;
	}

	public static IEvent makeChordRoutingPathEvent(List<IEvent> path, String source, String root){
		IEvent event = new Event(CHORD_ROUTE_EVENT);
		event.put(ROUTE_HOPS, path);
		event.put(ROUTING_EVENT_SOURCE, source);
		event.put(ROUTING_EVENT_DESTINATION, root);
		return event;
	}

	public static IEvent makeDOLRoutingPathEvent(List<IEvent> path, String source, String root) {
		IEvent event = new Event(DOL_ROUTE_EVENT);
		event.put(ROUTE_HOPS, path);
		event.put(ROUTING_EVENT_SOURCE, source);
		event.put(ROUTING_EVENT_DESTINATION, root);
		return event;
	}

	public static IEvent makeDOLResolutionEvent(String source, String root) {
		IEvent event = new Event(DOL_RESOLUTION_EVENT);
		event.put(ROUTING_EVENT_SOURCE, source);
		event.put(ROUTING_EVENT_DESTINATION, root);
		return event;
	}
}
