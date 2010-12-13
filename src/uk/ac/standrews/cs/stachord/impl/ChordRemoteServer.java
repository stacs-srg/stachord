package uk.ac.standrews.cs.stachord.impl;

import java.util.HashMap;
import java.util.Map;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class ChordRemoteServer extends ApplicationServer {

    private final ChordNodeImpl chord_node;
    private final Map<String, Handler> handler_map;
    private static final ChordRemoteMarshaller marshaller = new ChordRemoteMarshaller();

    public ChordRemoteServer(final ChordNodeImpl chord_node) {

        this.chord_node = chord_node;
        handler_map = new HashMap<String, Handler>();

        initHandlers();
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    Handler getHandler(final String method_name) {

        return handler_map.get(method_name);
    }

    // -------------------------------------------------------------------------------------------------------

    private void initHandlers() {

        handler_map.put("getKey", new Handler() {

            @Override
            public String execute(final String[] args) {

                return marshaller.serializeKey(chord_node.getKey());
            }
        });

        handler_map.put("getAddress", new Handler() {

            @Override
            public String execute(final String[] args) {

                return marshaller.serializeInetSocketAddress(chord_node.getAddress());
            }
        });

        handler_map.put("lookup", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                try {
                    final IKey key = marshaller.deserializeKey(args[0]);
                    return marshaller.serializeChordRemoteReference(chord_node.lookup(key));
                }
                catch (final DeserializationException e) {
                    throw new RemoteChordException(e);
                }
            }
        });

        handler_map.put("getSuccessor", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                return marshaller.serializeChordRemoteReference(chord_node.getSuccessor());
            }
        });

        handler_map.put("getPredecessor", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                return marshaller.serializeChordRemoteReference(chord_node.getPredecessor());
            }
        });

        handler_map.put("notify", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                try {
                    final IChordRemoteReference temp = marshaller.deserializeChordRemoteReference(args[0]);
                    final IChordRemoteReference potential_predecessor = temp;
                    chord_node.notify(potential_predecessor);
                    return "";
                }
                catch (final DeserializationException e) {
                    throw new RemoteChordException(e);
                }
            }
        });

        handler_map.put("join", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                try {
                    final IChordRemoteReference node = marshaller.deserializeChordRemoteReference(args[0]);
                    chord_node.join(node);
                    return "";
                }
                catch (final DeserializationException e) {
                    throw new RemoteChordException(e);
                }
            }
        });

        handler_map.put("getSuccessorList", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                return marshaller.serializeListChordRemoteReference(chord_node.getSuccessorList());
            }
        });

        handler_map.put("getFingerList", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                return marshaller.serializeListChordRemoteReference(chord_node.getFingerList());
            }
        });

        handler_map.put("isAlive", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                chord_node.isAlive();
                return "";
            }
        });

        handler_map.put("nextHop", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                try {
                    final IKey key = marshaller.deserializeKey(args[0]);
                    return marshaller.serializeNextHopResult(chord_node.nextHop(key));
                }
                catch (final DeserializationException e) {
                    throw new RemoteChordException(e);
                }
            }
        });

        handler_map.put("enablePredecessorMaintenance", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                try {
                    final boolean enabled = marshaller.deserializeBoolean(args[0]);
                    chord_node.enablePredecessorMaintenance(enabled);
                    return "";
                }
                catch (final DeserializationException e) {
                    throw new RemoteChordException(e);
                }
            }
        });

        handler_map.put("enableStabilization", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                try {
                    final boolean enabled = marshaller.deserializeBoolean(args[0]);
                    chord_node.enableStabilization(enabled);
                    return "";
                }
                catch (final DeserializationException e) {
                    throw new RemoteChordException(e);
                }
            }
        });

        handler_map.put("enablePeerStateMaintenance", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                try {
                    final boolean enabled = marshaller.deserializeBoolean(args[0]);
                    chord_node.enablePeerStateMaintenance(enabled);
                    return "";
                }
                catch (final DeserializationException e) {
                    throw new RemoteChordException(e);
                }
            }
        });

        handler_map.put("notifyFailure", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                try {
                    final IChordRemoteReference node = marshaller.deserializeChordRemoteReference(args[0]);
                    chord_node.notifyFailure(node);
                    return "";
                }
                catch (final DeserializationException e) {
                    throw new RemoteChordException(e);
                }
            }
        });

        handler_map.put("toStringDetailed", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                return chord_node.toStringDetailed();
            }
        });

        handler_map.put("toStringTerse", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                return chord_node.toStringTerse();
            }
        });

        // -------------------------------------------------------------------------------------------------------

        handler_map.put("hashCode", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                return marshaller.serializeInt(chord_node.hashCode());
            }
        });

        handler_map.put("toString", new Handler() {

            @Override
            public String execute(final String[] args) throws RemoteChordException {

                return chord_node.toString();
            }
        });
    }
}
