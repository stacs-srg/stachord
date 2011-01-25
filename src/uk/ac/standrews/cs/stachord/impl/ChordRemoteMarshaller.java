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

public class ChordRemoteMarshaller extends Marshaller {

    private static final String IS_FINAL_HOP_KEY = "isfinalhop";
    private static final String NODE_KEY = "node";
    private static final String PROXY_KEY = "proxy";
    private static final String KEY_KEY = "key";

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

    public IChordRemoteReference deserializeChordRemoteReference(final JSONObject object) throws DeserializationException {

        if (object == null) { return null; }

        try {

            final String serialized_key = object.getString(KEY_KEY);
            final String serialized_address = object.getString(PROXY_KEY);

            final InetSocketAddress address = deserializeInetSocketAddress(serialized_address);
            assert address.getAddress() != null;

            if (serialized_key.equals("")) { return new ChordRemoteReference(address); }

            final IKey key = deserializeKey(serialized_key);
            return new ChordRemoteReference(key, address);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    public JSONValue serializeListChordRemoteReference(final List<IChordRemoteReference> list_chord_remote_reference) {

        final JSONArray array = new JSONArray();

        for (final IChordRemoteReference reference : list_chord_remote_reference) {
            array.put(serializeChordRemoteReference(reference).getValue());
        }
        return new JSONValue(array);
    }

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
