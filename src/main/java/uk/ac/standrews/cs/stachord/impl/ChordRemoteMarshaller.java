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

import org.json.JSONException;
import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
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
     * @throws JSONException 
     * @throws RPCException 
     */
    public void serializeChordRemoteReference(final IChordRemoteReference chord_remote_reference, final JSONWriter writer) throws JSONException, RPCException {

        if (chord_remote_reference == null) {
            writer.value(null);
        }
        else {
            writer.object();

            writer.key(KEY_KEY);
            serializeKey(chord_remote_reference.getCachedKey(), writer);

            writer.key(PROXY_KEY);
            serializeInetSocketAddress(((ChordRemoteProxy) chord_remote_reference.getRemote()).getProxiedAddress(), writer);

            writer.endObject();
        }
    }

    /**
     * Deserializes a chord remote reference.
     *
     * @param object a JSON object containing the appropriate fields
     * @return a chord remote reference
     * @throws DeserializationException if the representation is invalid
     */
    public IChordRemoteReference deserializeChordRemoteReference(final JSONReader reader) throws DeserializationException {

        try {
            if (reader.checkNull()) { return null; }

            reader.object();

            reader.key(KEY_KEY);
            final IKey key = deserializeKey(reader);

            reader.key(PROXY_KEY);
            final InetSocketAddress address = deserializeInetSocketAddress(reader);

            reader.endObject();

            return new ChordRemoteReference(key, address);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Serializes a list of chord remote references to an array.
     *
     * @param list_chord_remote_reference the list
     * @return a JSON array
     * @throws JSONException 
     * @throws RPCException 
     */
    public void serializeListChordRemoteReference(final List<IChordRemoteReference> list_chord_remote_reference, final JSONWriter writer) throws JSONException, RPCException {

        if (list_chord_remote_reference != null) {
            writer.array();
            for (final IChordRemoteReference reference : list_chord_remote_reference) {
                serializeChordRemoteReference(reference, writer);
            }

            writer.endArray();
        }
        else {
            writer.value(null);
        }
    }

    /**
     * Deserializes a list of chord remote references.
     *
     * @param array a JSON array containing the appropriate values
     * @return a list of chord remote references
     * @throws DeserializationException if the representation is invalid
     */
    public List<IChordRemoteReference> deserializeListChordRemoteReference(final JSONReader reader) throws DeserializationException {

        try {
            if (reader.checkNull()) { return null; }

            reader.array();

            final List<IChordRemoteReference> deserialized_references = new ArrayList<IChordRemoteReference>();

            while (!reader.have(JSONReader.ENDARRAY)) {
                deserialized_references.add(deserializeChordRemoteReference(reader));
            }

            reader.endArray();
            return deserialized_references;
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Serializes a next hop result to an object containing the remote reference and the final hop flag.
     *
     * @param next_hop_result the next hop result
     * @return a JSON object
     * @throws JSONException 
     * @throws RPCException 
     */
    public void serializeNextHopResult(final NextHopResult next_hop_result, final JSONWriter writer) throws JSONException, RPCException {

        if (next_hop_result != null) {
            writer.object();

            writer.key(NODE_KEY);
            serializeChordRemoteReference(next_hop_result.getNode(), writer);

            writer.key(IS_FINAL_HOP_KEY);
            writer.value(next_hop_result.isFinalHop());

            writer.endObject();
        }
        else {
            writer.value(null);
        }
    }

    /**
     * Deserializes a next hop result.
     *
     * @param object a JSON object containing the appropriate fields
     * @return a next hop result
     * @throws DeserializationException if the representation is invalid
     */
    public NextHopResult deserializeNextHopResult(final JSONReader reader) throws DeserializationException {

        try {
            if (reader.checkNull()) { return null; }

            reader.object();

            reader.key(NODE_KEY);
            final IChordRemoteReference chordRemoteReference = deserializeChordRemoteReference(reader);

            reader.key(IS_FINAL_HOP_KEY);
            final boolean is_final_hop = reader.booleanValue();

            reader.endObject();

            return new NextHopResult(chordRemoteReference, is_final_hop);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }
}
