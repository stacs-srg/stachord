package uk.ac.standrews.cs.stachord.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.interfaces.RemoteException;

public class ChordRemoteProxy implements IChordRemote {

    private final InetSocketAddress node_address;
    private Socket socket;
    private BufferedReader input_stream;
    private PrintStream output_stream;
    private static final Marshaller marshaller = new Marshaller();

    // -------------------------------------------------------------------------------------------------------

    public ChordRemoteProxy(final InetSocketAddress node_address) {

        this.node_address = node_address;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public IKey getKey() throws RemoteException {

        return marshaller.deserializeKey(makeCall("getKey"));
    }

    @Override
    public InetSocketAddress getAddress() throws RemoteException {

        return marshaller.deserializeInetSocketAddress(makeCall("getAddress"));
    }

    @Override
    public IChordRemoteReference lookup(final IKey key) throws RemoteException {

        return marshaller.deserializeChordRemoteReference(makeCall("lookup", marshaller.serializeKey(key)));
    }

    @Override
    public IChordRemoteReference getSuccessor() throws RemoteException {

        return marshaller.deserializeChordRemoteReference(makeCall("getSuccessor"));
    }

    @Override
    public IChordRemoteReference getPredecessor() throws RemoteException {

        return marshaller.deserializeChordRemoteReference(makeCall("getPredecessor"));
    }

    @Override
    public void notify(final IChordRemoteReference potential_predecessor) throws RemoteException {

        makeCall("notify", marshaller.serializeChordRemoteReference(potential_predecessor));
    }

    @Override
    public void join(final IChordRemoteReference node) throws RemoteException {

        makeCall("join", marshaller.serializeChordRemoteReference(node));
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() throws RemoteException {

        return marshaller.deserializeListChordRemoteReference(makeCall("getSuccessorList"));
    }

    @Override
    public List<IChordRemoteReference> getFingerList() throws RemoteException {

        return marshaller.deserializeListChordRemoteReference(makeCall("getFingerList"));
    }

    @Override
    public void isAlive() throws RemoteException {

        makeCall("isAlive");
    }

    @Override
    public NextHopResult nextHop(final IKey key) throws RemoteException {

        return marshaller.deserializeNextHopResult(makeCall("nextHop", marshaller.serializeKey(key)));
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

    private String makeCall(final String method_name, final String... args) throws RemoteException {

        try {
            setupSocket();
            setupStreams();

            sendMethodName(method_name);
            sendArgs(args);

            return readReply();
        }
        catch (final IOException e) {
            throw new RemoteException(e);
        }
        finally {

            tearDownStreams();
            tearDownSocket();
        }
    }

    private void setupSocket() throws IOException {

        socket = new Socket(node_address.getAddress(), node_address.getPort());
    }

    private void setupStreams() throws IOException {

        input_stream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output_stream = new PrintStream(socket.getOutputStream(), true);
    }

    private void sendMethodName(final String method_name) {

        output_stream.println(method_name);
    }

    private void sendArgs(final String[] args) {

        for (final String arg : args) {
            output_stream.println(arg);
        }
        output_stream.println();
    }

    private String readReply() throws IOException {

        return input_stream.readLine();
    }

    private void tearDownStreams() {

        try {
            input_stream.close();
        }
        catch (final IOException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error closing input stream");
        }
        finally {
            output_stream.close();
        }
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
