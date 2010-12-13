package uk.ac.standrews.cs.stachord.impl;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.Proxy;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class ChordRemoteProxy extends Proxy implements IChordRemote {

    private static final Map<InetSocketAddress, ChordRemoteProxy> proxy_map;
    private static final ChordRemoteMarshaller marshaller;

    static {
        marshaller = new ChordRemoteMarshaller();
        proxy_map = new HashMap<InetSocketAddress, ChordRemoteProxy>();
    }

    // -------------------------------------------------------------------------------------------------------

    private ChordRemoteProxy(final InetSocketAddress node_address) {

        super(node_address);
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
    public IKey getKey() throws RPCException {

        try {
            return marshaller.deserializeKey(makeCall("getKey"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public InetSocketAddress getAddress() throws RPCException {

        try {
            return marshaller.deserializeInetSocketAddress(makeCall("getAddress"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public IChordRemoteReference lookup(final IKey key) throws RPCException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("lookup", marshaller.serializeKey(key)));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public IChordRemoteReference getSuccessor() throws RPCException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("getSuccessor"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public IChordRemoteReference getPredecessor() throws RPCException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("getPredecessor"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public void notify(final IChordRemoteReference potential_predecessor) throws RPCException {

        makeCall("notify", marshaller.serializeChordRemoteReference(potential_predecessor));
    }

    @Override
    public void join(final IChordRemoteReference node) throws RPCException {

        final String serialized_reference = marshaller.serializeChordRemoteReference(node);
        makeCall("join", serialized_reference);
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() throws RPCException {

        try {
            return marshaller.deserializeListChordRemoteReference(makeCall("getSuccessorList"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public List<IChordRemoteReference> getFingerList() throws RPCException {

        try {
            return marshaller.deserializeListChordRemoteReference(makeCall("getFingerList"));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public void isAlive() throws RPCException {

        makeCall("isAlive");
    }

    @Override
    public NextHopResult nextHop(final IKey key) throws RPCException {

        try {
            return marshaller.deserializeNextHopResult(makeCall("nextHop", marshaller.serializeKey(key)));
        }
        catch (final DeserializationException e) {
            throw new RemoteChordException(e);
        }
    }

    @Override
    public void enablePredecessorMaintenance(final boolean enabled) throws RPCException {

        makeCall("enablePredecessorMaintenance", marshaller.serializeBoolean(enabled));
    }

    @Override
    public void enableStabilization(final boolean enabled) throws RPCException {

        makeCall("enableStabilization", marshaller.serializeBoolean(enabled));
    }

    @Override
    public void enablePeerStateMaintenance(final boolean enabled) throws RPCException {

        makeCall("enablePeerStateMaintenance", marshaller.serializeBoolean(enabled));
    }

    @Override
    public void notifyFailure(final IChordRemoteReference node) throws RPCException {

        makeCall("notifyFailure", marshaller.serializeChordRemoteReference(node));
    }

    @Override
    public String toStringDetailed() throws RPCException {

        return makeCall("toStringDetailed");
    }

    @Override
    public String toStringTerse() throws RPCException {

        return makeCall("toStringTerse");
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(final Object o) {

        try {
            return o instanceof IChordRemote && ((IChordRemote) o).getKey().equals(getKey());
        }
        catch (final RPCException e) {
            return false;
        }
    }

    @Override
    public String toString() {

        try {
            return makeCall("toString");
        }
        catch (final RPCException e) {
            return "inaccessible";
        }
    }

    @Override
    public int hashCode() {

        try {
            return marshaller.deserializeInt(makeCall("hashCode"));
        }
        catch (final RPCException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error calling remote hashCode()");
            return 0;
        }
        catch (final DeserializationException e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error deserializing hashCode() result");
            return 0;
        }
    }
}
