package uk.ac.standrews.cs.stachord.impl;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;

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
            return marshaller.deserializeKey(makeCall("getKey").getString());
        }
        catch (final DeserializationException e) {
            throw new RPCException(e);
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public InetSocketAddress getAddress() throws RPCException {

        try {
            return marshaller.deserializeInetSocketAddress(makeCall("getAddress").getString());
        }
        catch (final DeserializationException e) {
            throw new RPCException(e);
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public IChordRemoteReference lookup(final IKey key) throws RPCException {

        try {
            final JSONArray args = new JSONArray();
            args.put(marshaller.serializeKey(key).getValue());
            return marshaller.deserializeChordRemoteReference(makeCall("lookup", args).getJSONObject());
        }
        catch (final DeserializationException e) {
            throw new RPCException(e);
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public IChordRemoteReference getSuccessor() throws RPCException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("getSuccessor").getJSONObject());
        }
        catch (final DeserializationException e) {
            throw new RPCException(e);
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public IChordRemoteReference getPredecessor() throws RPCException {

        try {
            return marshaller.deserializeChordRemoteReference(makeCall("getPredecessor").getJSONObject());
        }
        catch (final DeserializationException e) {
            throw new RPCException(e);
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public void notify(final IChordRemoteReference potential_predecessor) throws RPCException {

        try {
            final JSONArray args = new JSONArray();
            args.put(marshaller.serializeChordRemoteReference(potential_predecessor).getValue());
            makeCall("notify", args);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void join(final IChordRemoteReference node) throws RPCException {

        try {
            final JSONArray args = new JSONArray();
            args.put(marshaller.serializeChordRemoteReference(node).getValue());
            makeCall("join", args);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() throws RPCException {

        try {
            return marshaller.deserializeListChordRemoteReference(makeCall("getSuccessorList"));
        }
        catch (final DeserializationException e) {
            throw new RPCException(e);
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public List<IChordRemoteReference> getFingerList() throws RPCException {

        try {
            return marshaller.deserializeListChordRemoteReference(makeCall("getFingerList"));
        }
        catch (final DeserializationException e) {
            throw new RPCException(e);
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public void isAlive() throws RPCException {

        try {
            makeCall("isAlive");
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public NextHopResult nextHop(final IKey key) throws RPCException {

        try {
            final JSONArray args = new JSONArray();
            args.put(marshaller.serializeKey(key).getValue());
            return marshaller.deserializeNextHopResult(makeCall("nextHop", args).getJSONObject());
        }
        catch (final DeserializationException e) {
            throw new RPCException(e);
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public void enablePredecessorMaintenance(final boolean enabled) throws RPCException {

        try {
            final JSONArray args = new JSONArray();
            args.put(enabled);
            makeCall("enablePredecessorMaintenance", args);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void enableStabilization(final boolean enabled) throws RPCException {

        try {
            final JSONArray args = new JSONArray();
            args.put(enabled);
            makeCall("enableStabilization", args);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void enablePeerStateMaintenance(final boolean enabled) throws RPCException {

        try {
            final JSONArray args = new JSONArray();
            args.put(enabled);
            makeCall("enablePeerStateMaintenance", args);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void notifyFailure(final IChordRemoteReference node) throws RPCException {

        try {
            final JSONArray args = new JSONArray();
            args.put(marshaller.serializeChordRemoteReference(node).getValue());
            makeCall("notifyFailure", args);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public String toStringDetailed() throws RPCException {

        try {
            return makeCall("toStringDetailed").getString();
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public String toStringTerse() throws RPCException {

        try {
            return makeCall("toStringTerse").getString();
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
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
            return makeCall("toString").getString();
        }
        catch (final Exception e) {
            return "inaccessible";
        }
    }

    @Override
    public int hashCode() {

        try {
            return makeCall("hashCode").getInt();
        }
        catch (final Exception e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error calling remote hashCode()");
            return 0;
        }
    }
}
