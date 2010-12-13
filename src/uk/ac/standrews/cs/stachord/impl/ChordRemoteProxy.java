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

    private static final int SOCKET_CREATION_TIMEOUT = 5000;
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
    public IKey getKey() throws RemoteChordException {

        try {
            return marshaller.deserializeKey(makeCall("getKey"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public InetSocketAddress getAddress() throws RemoteChordException {

        try {
            return marshaller.deserializeInetSocketAddress(makeCall("getAddress"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public IChordRemoteReference lookup(final IKey key) throws RemoteChordException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("lookup", marshaller.serializeKey(key)));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public IChordRemoteReference getSuccessor() throws RemoteChordException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("getSuccessor"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public IChordRemoteReference getPredecessor() throws RemoteChordException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("getPredecessor"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public void notify(final IChordRemoteReference potential_predecessor) throws RemoteChordException {

        makeCall("notify", marshaller.serializeChordRemoteReference(potential_predecessor));
    }

    @Override
    public void join(final IChordRemoteReference node) throws RemoteChordException {

        final String serialized_reference = marshaller.serializeChordRemoteReference(node);
        makeCall("join", serialized_reference);
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() throws RemoteChordException {

        try {
            return marshaller.deserializeListChordRemoteReference(makeCall("getSuccessorList"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public List<IChordRemoteReference> getFingerList() throws RemoteChordException {

        try {
            return marshaller.deserializeListChordRemoteReference(makeCall("getFingerList"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public void isAlive() throws RemoteChordException {

        makeCall("isAlive");
    }

    @Override
    public NextHopResult nextHop(final IKey key) throws RemoteChordException {

        try {
            return marshaller.deserializeNextHopResult(makeCall("nextHop", marshaller.serializeKey(key)));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public void enablePredecessorMaintenance(final boolean enabled) throws RemoteChordException {

        makeCall("enablePredecessorMaintenance", marshaller.serializeBoolean(enabled));
    }

    @Override
    public void enableStabilization(final boolean enabled) throws RemoteChordException {

        makeCall("enableStabilization", marshaller.serializeBoolean(enabled));
    }

    @Override
    public void enablePeerStateMaintenance(final boolean enabled) throws RemoteChordException {

        makeCall("enablePeerStateMaintenance", marshaller.serializeBoolean(enabled));
    }

    @Override
    public void notifyFailure(final IChordRemoteReference node) throws RemoteChordException {

        makeCall("notifyFailure", marshaller.serializeChordRemoteReference(node));
    }

    @Override
    public String toStringDetailed() throws RemoteChordException {

        return makeCall("toStringDetailed");
    }

    @Override
    public String toStringTerse() throws RemoteChordException {

        return makeCall("toStringTerse");
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(final Object o) {

        try {
            return o instanceof IChordRemote && ((IChordRemote) o).getKey().equals(getKey());
        }
        catch (final RemoteChordException e) {
            return false;
        }
    }

    @Override
    public String toString() {

        try {
            return makeCall("toString");
        }
        catch (final RemoteChordException e) {
            return "inaccessible";
        }
    }

    @Override
    public int hashCode() {

        try {
            return marshaller.deserializeInt(makeCall("hashCode"));
        }
        catch (final RemoteChordException e) {
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

    private synchronized String makeCall(final String method_name, final String... args) throws RemoteChordException {

        try {
            System.out.println("mc1");
            setupSocket();
            System.out.println("mc2");
            setupStreams();
            System.out.println("mc3");

            sendMethodName(method_name);
            System.out.println("mc4");
            sendArgs(args);
            System.out.println("mc5");

            final String reply = readReply();
            System.out.println("mc6");

            if (reply == null || reply.startsWith("exception")) { throw new RemoteChordException(reply); }
            System.out.println("mc7");
            return reply;
        }
        catch (final IOException e) {
            System.out.println("mc8");
            throw new RemoteChordException(e);
        }
        finally {

            try {
                System.out.println("mc9");
                tearDownStreams();
                System.out.println("mc10");
            }
            finally {
                System.out.println("mc11");
                tearDownSocket();
                System.out.println("mc12");
            }
        }
    }

    private void setupSocket() throws IOException {

        System.out.println("ss1");
        final InetAddress address = node_address.getAddress();
        System.out.println("address: " + address);
        final int port = node_address.getPort();
        System.out.println("port: " + port);

        socket = new Socket();
        socket.connect(new InetSocketAddress(address, port), SOCKET_CREATION_TIMEOUT);
        System.out.println("ss2");
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
