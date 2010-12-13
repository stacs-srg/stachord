package uk.ac.standrews.cs.stachord.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class ChordRemoteProxy implements IChordRemote {

    private final InetSocketAddress node_address;
    private Socket socket;
    private StreamPair streams;
    private static final ChordRemoteMarshaller marshaller;
    private static final Map<InetSocketAddress, ChordRemoteProxy> proxy_map;

    static {
        marshaller = new ChordRemoteMarshaller();
        proxy_map = new HashMap<InetSocketAddress, ChordRemoteProxy>();
    }

    // -------------------------------------------------------------------------------------------------------

    private ChordRemoteProxy(final InetSocketAddress node_address) {

        this.node_address = node_address;
    }

    // -------------------------------------------------------------------------------------------------------

    public static synchronized ChordRemoteProxy getProxy(final InetSocketAddress proxy_address) {

        ChordRemoteProxy proxy = proxy_map.get(proxy_address);
        if (proxy == null) {
            proxy = new ChordRemoteProxy(proxy_address);
            proxy_map.put(proxy_address, proxy);
        }
        return proxy;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public IKey getKey() throws RemoteException {

        try {
            return marshaller.deserializeKey(makeCall("getKey"));
        }
        catch (final DeserializationException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public InetSocketAddress getAddress() throws RemoteException {

        try {
            return marshaller.deserializeInetSocketAddress(makeCall("getAddress"));
        }
        catch (final DeserializationException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public IChordRemoteReference lookup(final IKey key) throws RemoteException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("lookup", marshaller.serializeKey(key)));
        }
        catch (final DeserializationException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public IChordRemoteReference getSuccessor() throws RemoteException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("getSuccessor"));
        }
        catch (final DeserializationException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public IChordRemoteReference getPredecessor() throws RemoteException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("getPredecessor"));
        }
        catch (final DeserializationException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public void notify(final IChordRemoteReference potential_predecessor) throws RemoteException {

        makeCall("notify", marshaller.serializeChordRemoteReference(potential_predecessor));
    }

    @Override
    public void join(final IChordRemoteReference node) throws RemoteException {

        final String serialized_reference = marshaller.serializeChordRemoteReference(node);
        makeCall("join", serialized_reference);
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() throws RemoteException {

        try {
            return marshaller.deserializeListChordRemoteReference(makeCall("getSuccessorList"));
        }
        catch (final DeserializationException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public List<IChordRemoteReference> getFingerList() throws RemoteException {

        try {
            return marshaller.deserializeListChordRemoteReference(makeCall("getFingerList"));
        }
        catch (final DeserializationException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public void isAlive() throws RemoteException {

        makeCall("isAlive");
    }

    @Override
    public NextHopResult nextHop(final IKey key) throws RemoteException {

        try {
            return marshaller.deserializeNextHopResult(makeCall("nextHop", marshaller.serializeKey(key)));
        }
        catch (final DeserializationException e) {
            throw new RemoteException(e);
        }
    }

    @Override
    public void enablePredecessorMaintenance(final boolean enabled) throws RemoteException {

        makeCall("enablePredecessorMaintenance", marshaller.serializeBoolean(enabled));
    }

    @Override
    public void enableStabilization(final boolean enabled) throws RemoteException {

        makeCall("enableStabilization", marshaller.serializeBoolean(enabled));
    }

    @Override
    public void enablePeerStateMaintenance(final boolean enabled) throws RemoteException {

        makeCall("enablePeerStateMaintenance", marshaller.serializeBoolean(enabled));
    }

    @Override
    public void notifyFailure(final IChordRemoteReference node) throws RemoteException {

        makeCall("notifyFailure", marshaller.serializeChordRemoteReference(node));
    }

    @Override
    public String toStringDetailed() throws RemoteException {

        return makeCall("toStringDetailed");
    }

    @Override
    public String toStringTerse() throws RemoteException {

        return makeCall("toStringTerse");
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(final Object o) {

        try {
            return o instanceof IChordRemote && ((IChordRemote) o).getKey().equals(getKey());
        }
        catch (final RemoteException e) {
            return false;
        }
    }

    @Override
    public String toString() {

        try {
            return makeCall("toString");
        }
        catch (final RemoteException e) {
            return "inaccessible";
        }
    }

    @Override
    public int hashCode() {

        try {
            return marshaller.deserializeInt(makeCall("hashCode"));
        }
        catch (final RemoteException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error calling remote hashCode()");
            return 0;
        }
        catch (final DeserializationException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error deserializing hashCode() result");
            return 0;
        }
    }

    // -------------------------------------------------------------------------------------------------------

    public InetSocketAddress getProxiedAddress() {

        return node_address;
    }

    // -------------------------------------------------------------------------------------------------------

    private synchronized String makeCall(final String method_name, final String... args) throws RemoteException {

        try {
            setupSocket();
            setupStreams();

            sendMethodName(method_name);
            sendArgs(args);

            final String reply = readReply();

            if (reply == null || reply.startsWith("exception")) { throw new RemoteException(reply); }
            return reply;
        }
        catch (final IOException e) {
            throw new RemoteException(e);
        }
        finally {

            try {
                tearDownStreams();
            }
            finally {
                tearDownSocket();
            }
        }
    }

    private void setupSocket() throws IOException {

        final InetAddress address = node_address.getAddress();
        final int port = node_address.getPort();

        socket = new Socket(address, port);
    }

    private void setupStreams() throws IOException {

        streams = new StreamPair(socket);
    }

    private void tearDownStreams() {

        if (streams != null) {
            streams.tearDownStreams();
            streams = null;
        }
    }

    private void sendMethodName(final String method_name) {

        streams.println(method_name);
    }

    private void sendArgs(final String[] args) {

        for (final String arg : args) {
            streams.println(arg);
        }
        streams.println();
    }

    private String readReply() throws IOException {

        return streams.readLine();
    }

    private void tearDownSocket() {

        if (socket != null) {
            try {
                socket.close();
            }
            catch (final IOException e) {
                Diagnostic.trace(DiagnosticLevel.RUN, "error closing client socket");
            }
            socket = null;
        }
    }
}
