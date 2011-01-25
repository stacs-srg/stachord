/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://www-systems.cs.st-andrews.ac.uk/                                 *
 *                                                                         *
 * This file is part of stachord, an independent implementation of         *
 * the Chord protocol (http://pdos.csail.mit.edu/chord/).                  *
 *                                                                         *
 * stachord is free software: you can redistribute it and/or modify        *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation, either version 3 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * stachord is distributed in the hope that it will be useful,             *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *
 *                                                                         *
 * You should have received a copy of the GNU General Public License       *
 * along with stachord.  If not, see <http://www.gnu.org/licenses/>.       *
 *                                                                         *
 ***************************************************************************/
package uk.ac.standrews.cs.stachord.impl;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.ApplicationServer;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.Handler;
import uk.ac.standrews.cs.nds.rpc.JSONValue;
import uk.ac.standrews.cs.nds.rpc.Marshaller;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Chord-specific server-side RPC mechanism.
 *
 * @author Graham Kirby (graham@cs.st-andrews.ac.uk)
 */
public class ChordRemoteServer extends ApplicationServer {

    private final ChordNodeImpl chord_node;
    private final Map<String, Handler> handler_map;
    private final ChordRemoteMarshaller marshaller;

    /**
     * Initialises a server for a given Chord node.
     *
     * @param chord_node the Chord node
     */
    public ChordRemoteServer(final ChordNodeImpl chord_node) {

        this.chord_node = chord_node;
        handler_map = new HashMap<String, Handler>();

        marshaller = new ChordRemoteMarshaller();
        initHandlers();
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public Handler getHandler(final String method_name) {

        return handler_map.get(method_name);
    }

    @Override
    public Marshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------

    private void initHandlers() {

        handler_map.put("getKey", new GetKeyHandler());
        handler_map.put("getAddress", new GetAddressHandler());
        handler_map.put("lookup", new LookupHandler());
        handler_map.put("getSuccessor", new GetSuccessorHandler());
        handler_map.put("getPredecessor", new GetPredecessorHandler());
        handler_map.put("notify", new NotifyHandler());
        handler_map.put("join", new JoinHandler());
        handler_map.put("getSuccessorList", new GetSuccessorListHandler());
        handler_map.put("getFingerList", new GetFingerListHandler());
        handler_map.put("isAlive", new IsAliveHandler());
        handler_map.put("nextHop", new NextHopHandler());
        handler_map.put("enablePredecessorMaintenance", new EnablePredecessorMaintenanceHandler());
        handler_map.put("enableStabilization", new EnableStabilizationHandler());
        handler_map.put("enablePeerStateMaintenance", new EnablePeerStateMaintenanceHandler());
        handler_map.put("notifyFailure", new NotifyFailureHandler());
        handler_map.put("toStringDetailed", new ToStringDetailedHandler());
        handler_map.put("toStringTerse", new ToStringTerseHandler());
        handler_map.put("hashCode", new HashCodeHandler());
        handler_map.put("toString", new ToStringHandler());
    }

    // -------------------------------------------------------------------------------------------------------

    private final class GetKeyHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return marshaller.serializeKey(chord_node.getKey());
        }
    }

    private final class GetAddressHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return marshaller.serializeInetSocketAddress(chord_node.getAddress());
        }
    }

    private final class LookupHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) throws RPCException {

            try {
                final IKey key = marshaller.deserializeKey(args.getString(0));
                return marshaller.serializeChordRemoteReference(chord_node.lookup(key));
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class GetSuccessorHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return marshaller.serializeChordRemoteReference(chord_node.getSuccessor());
        }
    }

    private final class GetPredecessorHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return marshaller.serializeChordRemoteReference(chord_node.getPredecessor());
        }
    }

    private final class NotifyHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) throws RPCException {

            try {
                final IChordRemoteReference temp = marshaller.deserializeChordRemoteReference(args.getJSONObject(0));
                final IChordRemoteReference potential_predecessor = temp;
                chord_node.notify(potential_predecessor);
                return JSONValue.NULL;
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class JoinHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) throws RPCException {

            try {
                final IChordRemoteReference node = marshaller.deserializeChordRemoteReference(args.getJSONObject(0));
                chord_node.join(node);
                return JSONValue.NULL;
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class GetSuccessorListHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return marshaller.serializeListChordRemoteReference(chord_node.getSuccessorList());
        }
    }

    private final class GetFingerListHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return marshaller.serializeListChordRemoteReference(chord_node.getFingerList());
        }
    }

    private final class IsAliveHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            chord_node.isAlive();
            return JSONValue.NULL;
        }
    }

    private final class NextHopHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) throws RPCException {

            try {
                final IKey key = marshaller.deserializeKey(args.getString(0));
                return marshaller.serializeNextHopResult(chord_node.nextHop(key));
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class EnablePredecessorMaintenanceHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) throws RPCException {

            try {
                final boolean enabled = args.getBoolean(0);
                chord_node.enablePredecessorMaintenance(enabled);
                return JSONValue.NULL;
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class EnableStabilizationHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) throws RPCException {

            try {
                final boolean enabled = args.getBoolean(0);
                chord_node.enableStabilization(enabled);
                return JSONValue.NULL;
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class EnablePeerStateMaintenanceHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) throws RPCException {

            try {
                final boolean enabled = args.getBoolean(0);
                chord_node.enablePeerStateMaintenance(enabled);
                return JSONValue.NULL;
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class NotifyFailureHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) throws RPCException {

            try {
                final IChordRemoteReference node = marshaller.deserializeChordRemoteReference(args.getJSONObject(0));
                chord_node.notifyFailure(node);
                return JSONValue.NULL;
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class ToStringDetailedHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return new JSONValue(chord_node.toStringDetailed());
        }
    }

    private final class ToStringTerseHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return new JSONValue(chord_node.toStringTerse());
        }
    }

    private final class HashCodeHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return new JSONValue(chord_node.hashCode());
        }
    }

    private final class ToStringHandler implements Handler {

        @Override
        public JSONValue execute(final JSONArray args) {

            return new JSONValue(chord_node.toString());
        }
    }
}
