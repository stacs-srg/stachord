package uk.ac.standrews.cs.stachord.impl;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.Marshaller;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class ChordRemoteMarshaller extends Marshaller {

    public String serializeChordRemoteReference(final IChordRemoteReference chord_remote_reference) {

        if (chord_remote_reference == null) { return NULL; }

        String serialized_key = "";
        try {
            serialized_key = serializeKey(chord_remote_reference.getCachedKey());
        }
        catch (final RPCException e) {
            // Ignore.
        }

        final String serialized_proxy = serializeInetSocketAddress(((ChordRemoteProxy) chord_remote_reference.getRemote()).getProxiedAddress());

        return concatenate("*", serialized_key, serialized_proxy);
    }

    public IChordRemoteReference deserializeChordRemoteReference(final String serialized_chord_remote_reference) throws DeserializationException {

        if (serialized_chord_remote_reference.equals(NULL)) { return null; }

        try {
            final String[] components = serialized_chord_remote_reference.split("\\*", -1);
            final InetSocketAddress address = deserializeInetSocketAddress(components[1]);

            assert address.getAddress() != null;

            if (components[0].equals("")) { return new ChordRemoteReference(address); }

            final IKey key = deserializeKey(components[0]);
            return new ChordRemoteReference(key, address);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    public String serializeListChordRemoteReference(final List<IChordRemoteReference> list_chord_remote_reference) {

        final List<String> serialized_references = new ArrayList<String>();
        for (final IChordRemoteReference reference : list_chord_remote_reference) {
            serialized_references.add(serializeChordRemoteReference(reference));
        }
        return concatenate("%", serialized_references.toArray(new String[0]));
    }

    public List<IChordRemoteReference> deserializeListChordRemoteReference(final String serialized_list_chord_remote_reference) throws DeserializationException {

        final List<IChordRemoteReference> deserialized_references = new ArrayList<IChordRemoteReference>();

        if (serialized_list_chord_remote_reference.length() > 0) {
            final String[] components = serialized_list_chord_remote_reference.split("%", -1);

            try {
                for (final String serialized_chord_remote_reference : components) {
                    deserialized_references.add(deserializeChordRemoteReference(serialized_chord_remote_reference));
                }
            }
            catch (final Exception e) {
                throw new DeserializationException(e);
            }
        }

        return deserialized_references;
    }

    public String serializeNextHopResult(final NextHopResult next_hop_result) {

        return concatenate("%", serializeChordRemoteReference(next_hop_result.getNode()), serializeBoolean(next_hop_result.isFinalHop()));
    }

    public NextHopResult deserializeNextHopResult(final String serialized_next_hop_result) throws DeserializationException {

        final String[] components = serialized_next_hop_result.split("%", -1);
        try {
            return new NextHopResult(deserializeChordRemoteReference(components[0]), deserializeBoolean(components[1]));
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }
}
