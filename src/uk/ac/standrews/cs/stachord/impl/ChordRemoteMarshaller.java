package uk.ac.standrews.cs.stachord.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.impl.Key;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class ChordRemoteMarshaller {

    private static final String NULL = "null";

    public String serializeKey(final IKey key) {

        return key.toString(Key.DEFAULT_RADIX);
    }

    public IKey deserializeKey(final String serialized_key) throws DeserializationException {

        try {
            return new Key(serialized_key);
        }
        catch (final NumberFormatException e) {
            throw new DeserializationException(e);
        }
    }

    public String serializeInetSocketAddress(final InetSocketAddress inet_socket_address) {

        return inet_socket_address.toString();
    }

    public InetSocketAddress deserializeInetSocketAddress(final String serialized_inet_socket_address) throws DeserializationException {

        final String[] components = serialized_inet_socket_address.split(":", -1);
        try {
            final String host = components[0];
            final int port = Integer.parseInt(components[1]);

            final String name = getName(host);
            final byte[] address_bytes = getBytes(host);

            final InetAddress addr = name.equals("") ? InetAddress.getByAddress(address_bytes) : InetAddress.getByAddress(name, address_bytes);

            return new InetSocketAddress(addr, port);
        }
        catch (final Exception e) {
            throw new DeserializationException(e);
        }
    }

    private String getName(final String host) {

        final String[] name_address = host.split("/", -1);
        return name_address[0];
    }

    private byte[] getBytes(final String host) {

        final String[] name_address = host.split("/", -1);
        final String[] byte_strings = name_address[1].split("\\.", -1);
        final byte[] bytes = new byte[byte_strings.length];

        for (int i = 0; i < byte_strings.length; i++) {
            final Integer j = Integer.valueOf(byte_strings[i]);
            bytes[i] = j.byteValue();
        }

        return bytes;
    }

    public String serializeChordRemoteReference(final IChordRemoteReference chord_remote_reference) {

        if (chord_remote_reference == null) { return NULL; }

        String serialized_key = "";
        try {
            serialized_key = serializeKey(chord_remote_reference.getCachedKey());
        }
        catch (final RemoteException e) {
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

    public String serializeBoolean(final boolean bool) {

        return bool ? "true" : "false";
    }

    public boolean deserializeBoolean(final String serialized_boolean) throws DeserializationException {

        if (serialized_boolean.equals("true")) { return true; }
        if (serialized_boolean.equals("false")) { return false; }

        throw new DeserializationException("invalid boolean");
    }

    public String serializeInt(final int i) {

        return String.valueOf(i);
    }

    public int deserializeInt(final String serialized_int) throws DeserializationException {

        try {
            return Integer.parseInt(serialized_int);
        }
        catch (final NumberFormatException e) {
            throw new DeserializationException(e);
        }
    }

    private String concatenate(final String delimiter, final String... components) {

        final StringBuilder builder = new StringBuilder();
        for (final String component : components) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(component);
        }
        return builder.toString();
    }
}
