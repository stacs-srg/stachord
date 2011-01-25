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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.JSONValue;
import uk.ac.standrews.cs.nds.rpc.Marshaller;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Provides methods serializing and deserializing Chord types to/from JSON strings.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ChordRemoteMarshaller extends Marshaller {

    private static final String IS_FINAL_HOP_KEY = "isfinalhop";
    private static final String NODE_KEY = "node";
    private static final String PROXY_KEY = "proxy";
    private static final String KEY_KEY = "key";

    /**
     * Serializes a chord remote reference to an object containing the key and the network address.
     *
     * @param chord_remote_reference the remote reference
     * @return a JSON object
     */
    public JSONValue serializeChordRemoteReference(final IChordRemoteReference chord_remote_reference) {

        if (chord_remote_reference == null) { return JSONValue.NULL; }

        JSONValue serialized_key = JSONValue.NULL;
        try {
            serialized_key = serializeKey(chord_remote_reference.getCachedKey());
        }
        catch (final RPCException e) {
            // Ignore.
        }

        final JSONValue serialized_proxy = serializeInetSocketAddress(((ChordRemoteProxy) chord_remote_reference.getRemote()).getProxiedAddress());

        final JSONObject object = new JSONObject();
        try {
            object.put(KEY_KEY, serialized_key.getValue());
            object.put(PROXY_KEY, serialized_proxy.getValue());
        }
        catch (final JSONException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error serializing IChordRemoteReference: " + e.getMessage());
        }

        return new JSONValue(object);
    }

    /**
     * Deserializes a chord remote reference.
     *
     * @param object a JSON object containing the appropriate fields
     * @return a chord remote reference
     * @throws DeserializationException if the representation is invalid
     */
    public IChordRemoteReference deserializeChordRemoteReference(final JSONObject object) throws DeserializationException {

        if (object == null) { return null; }

        try {

            final String serialized_key = object.getString(KEY_KEY);
            final String serialized_address = object.getString(PROXY_KEY);

            final InetSocketAddress address = deserializeInetSocketAddress(serialized_address);

            if (serialized_key.equals("")) { return new ChordRemoteReference(address); }

            final IKey key = deserializeKey(serialized_key);
            return new ChordRemoteReference(key, address);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    /**
     * Serializes a list of chord remote references to an array.
     *
     * @param list_chord_remote_reference the list
     * @return a JSON array
     */
    public JSONValue serializeListChordRemoteReference(final List<IChordRemoteReference> list_chord_remote_reference) {

        final JSONArray array = new JSONArray();

        for (final IChordRemoteReference reference : list_chord_remote_reference) {
            array.put(serializeChordRemoteReference(reference).getValue());
        }
        return new JSONValue(array);
    }

    /**
     * Deserializes a list of chord remote references.
     *
     * @param array a JSON array containing the appropriate values
     * @return a list of chord remote references
     * @throws DeserializationException if the representation is invalid
     */
    public List<IChordRemoteReference> deserializeListChordRemoteReference(final JSONArray array) throws DeserializationException {

        final List<IChordRemoteReference> deserialized_references = new ArrayList<IChordRemoteReference>();

        try {

            for (int i = 0; i < array.length(); i++) {
                final JSONObject serialized_chord_remote_reference = array.getJSONObject(i);
                deserialized_references.add(deserializeChordRemoteReference(serialized_chord_remote_reference));
            }

            return deserialized_references;
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    /**
     * Serializes a next hop result to an object containing the remote reference and the final hop flag.
     *
     * @param next_hop_result the next hop result
     * @return a JSON object
     */
    public JSONValue serializeNextHopResult(final NextHopResult next_hop_result) {

        final JSONObject object = new JSONObject();
        try {
            object.put(NODE_KEY, serializeChordRemoteReference(next_hop_result.getNode()).getValue());
            object.put(IS_FINAL_HOP_KEY, next_hop_result.isFinalHop());
        }
        catch (final JSONException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error serializing IChordRemoteReference: " + e.getMessage());
        }

        return new JSONValue(object);
    }

    /**
     * Deserializes a next hop result.
     *
     * @param object a JSON object containing the appropriate fields
     * @return a next hop result
     * @throws DeserializationException if the representation is invalid
     */
    public NextHopResult deserializeNextHopResult(final JSONObject object) throws DeserializationException {

        try {
            final JSONObject serialized_node = object.getJSONObject(NODE_KEY);
            final boolean is_final_hop = object.getBoolean(IS_FINAL_HOP_KEY);

            return new NextHopResult(deserializeChordRemoteReference(serialized_node), is_final_hop);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }
}
