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

import org.json.JSONException;
import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.ApplicationServer;
import uk.ac.standrews.cs.nds.rpc.stream.IHandler;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Chord-specific server-side RPC mechanism.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ChordRemoteServer extends ApplicationServer {

    public static final String DEFAULT_REGISTRY_KEY = "CHORD";

    private final ChordNodeImpl chord_node;
    private final ChordRemoteMarshaller marshaller;

    // -------------------------------------------------------------------------------------------------------

    /**
     * Initialises a server for a given Chord node.
     *
     * @param chord_node the Chord node
     */
    public ChordRemoteServer(final ChordNodeImpl chord_node) {

        this(chord_node, DEFAULT_REGISTRY_KEY);
    }

    /**
     * Initialises a server for a given Chord node, using a custom registry key.
     * This is only needed if multiple Chord nodes are to be run on a physical machine.
     *
     * @param chord_node the Chord node
     * @param registry_key the registry key
     */
    public ChordRemoteServer(final ChordNodeImpl chord_node, final String registry_key) {

        super();
        this.chord_node = chord_node;
        this.registry_key = registry_key;

        marshaller = new ChordRemoteMarshaller();
        initHandlers();
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public Marshaller getMarshaller() {

        return marshaller;
    }

    @Override
    public String getApplicationRegistryKey() {

        return registry_key;
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

    private final class GetKeyHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException {

            marshaller.serializeKey(chord_node.getKey(), writer);
        }
    }

    private final class GetAddressHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException {

            marshaller.serializeInetSocketAddress(chord_node.getAddress(), writer);
        }
    }

    private final class LookupHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws RPCException {

            try {
                final IKey key = marshaller.deserializeKey(args);
                marshaller.serializeChordRemoteReference(chord_node.lookup(key), writer);
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class GetSuccessorHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException, RPCException {

            marshaller.serializeChordRemoteReference(chord_node.getSuccessor(), writer);
        }
    }

    private final class GetPredecessorHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException, RPCException {

            marshaller.serializeChordRemoteReference(chord_node.getPredecessor(), writer);
        }
    }

    private final class NotifyHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws RPCException, JSONException {

            try {
                final IChordRemoteReference potential_predecessor = marshaller.deserializeChordRemoteReference(args);

                chord_node.notify(potential_predecessor);
                writer.value("");
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class JoinHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws RPCException, JSONException {

            try {
                final IChordRemoteReference node = marshaller.deserializeChordRemoteReference(args);
                chord_node.join(node);
                writer.value("");
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class GetSuccessorListHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException, RPCException {

            marshaller.serializeListChordRemoteReference(chord_node.getSuccessorList(), writer);
        }
    }

    private final class GetFingerListHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException, RPCException {

            marshaller.serializeListChordRemoteReference(chord_node.getFingerList(), writer);
        }
    }

    private final class NextHopHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws RPCException {

            try {
                final IKey key = marshaller.deserializeKey(args);
                marshaller.serializeNextHopResult(chord_node.nextHop(key), writer);
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class EnablePredecessorMaintenanceHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws RPCException {

            try {
                final boolean enabled = args.booleanValue();
                chord_node.enablePredecessorMaintenance(enabled);
                writer.value("");
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class EnableStabilizationHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws RPCException {

            try {
                final boolean enabled = args.booleanValue();
                chord_node.enableStabilization(enabled);
                writer.value("");
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class EnablePeerStateMaintenanceHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws RPCException {

            try {
                final boolean enabled = args.booleanValue();
                chord_node.enablePeerStateMaintenance(enabled);
                writer.value("");
            }
            catch (final JSONException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class NotifyFailureHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws RPCException, JSONException {

            try {
                final IChordRemoteReference node = marshaller.deserializeChordRemoteReference(args);
                chord_node.notifyFailure(node);
                writer.value("");
            }
            catch (final DeserializationException e) {
                throw new RemoteChordException(e);
            }
        }
    }

    private final class ToStringDetailedHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException {

            writer.value(chord_node.toStringDetailed());
        }
    }

    private final class ToStringTerseHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException {

            writer.value(chord_node.toStringTerse());
        }
    }

    private final class HashCodeHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException {

            writer.value(chord_node.hashCode());
        }
    }

    private final class ToStringHandler implements IHandler {

        @Override
        public void execute(final JSONReader args, final JSONWriter writer) throws JSONException {

            writer.value(chord_node.toString());
        }
    }
}
