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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.JSONstream.rpc.IStreamPair;
import uk.ac.standrews.cs.nds.JSONstream.rpc.JSONReader;
import uk.ac.standrews.cs.nds.JSONstream.rpc.Marshaller;
import uk.ac.standrews.cs.nds.JSONstream.rpc.Proxy;
import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Proxy for remotely accessible Chord node.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class ChordRemoteProxy extends Proxy implements IChordRemote {

    private static final Map<InetSocketAddress, ChordRemoteProxy> PROXY_MAP;

    static {
        PROXY_MAP = new HashMap<InetSocketAddress, ChordRemoteProxy>();
    }

    private final ChordRemoteMarshaller marshaller;

    // -------------------------------------------------------------------------------------------------------

    private ChordRemoteProxy(final InetSocketAddress node_address) {

        super(node_address);
        marshaller = new ChordRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------

    static synchronized ChordRemoteProxy getProxy(final InetSocketAddress proxy_address) {

        ChordRemoteProxy proxy = PROXY_MAP.get(proxy_address);
        if (proxy == null) {
            proxy = new ChordRemoteProxy(proxy_address);
            PROXY_MAP.put(proxy_address, proxy);
        }
        return proxy;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public Marshaller getMarshaller() {

        return marshaller;
    }

    // -------------------------------------------------------------------------------------------------------

    @Override
    public IKey getKey() throws RPCException {

        try {

            final IStreamPair streams = startCall("getKey");

            final JSONReader reader = makeCall(streams);
            final IKey result = marshaller.deserializeKey(reader);

            finishCall(streams);

            return result;

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

            final IStreamPair streams = startCall("getAddress");

            final JSONReader reader = makeCall(streams);
            final InetSocketAddress result = marshaller.deserializeInetSocketAddress(reader);

            finishCall(streams);

            return result;

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

            final IStreamPair streams = startCall("lookup");

            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeKey(key, writer);

            final JSONReader reader = makeCall(streams);
            final IChordRemoteReference result = marshaller.deserializeChordRemoteReference(reader);

            finishCall(streams);

            return result;

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
            final IStreamPair streams = startCall("getSuccessor");

            final JSONReader reader = makeCall(streams);
            final IChordRemoteReference result = marshaller.deserializeChordRemoteReference(reader);

            finishCall(streams);

            return result;

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

            final IStreamPair streams = startCall("getPredecessor");

            final JSONReader reader = makeCall(streams);

            final IChordRemoteReference result = marshaller.deserializeChordRemoteReference(reader);

            finishCall(streams);

            return result;
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

            final IStreamPair streams = startCall("notify");

            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeChordRemoteReference(potential_predecessor, writer);

            handleVoidCall(makeCall(streams));

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void join(final IChordRemoteReference node) throws RPCException {

        try {

            final IStreamPair streams = startCall("join");
            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeChordRemoteReference(node, writer);

            handleVoidCall(makeCall(streams));

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() throws RPCException {

        try {

            final IStreamPair streams = startCall("getSuccessorList");

            final JSONReader reader = makeCall(streams);
            final List<IChordRemoteReference> result = marshaller.deserializeListChordRemoteReference(reader);

            finishCall(streams);

            return result;

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
            final IStreamPair streams = startCall("getFingerList");

            final JSONReader reader = makeCall(streams);
            final List<IChordRemoteReference> result = marshaller.deserializeListChordRemoteReference(reader);

            finishCall(streams);

            return result;
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
    public NextHopResult nextHop(final IKey key) throws RPCException {

        try {

            final IStreamPair streams = startCall("nextHop");
            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeKey(key, writer);

            final JSONReader reader = makeCall(streams);
            final NextHopResult result = marshaller.deserializeNextHopResult(reader);

            finishCall(streams);

            return result;
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

            final IStreamPair streams = startCall("enablePredecessorMaintenance");
            final JSONWriter writer = streams.getJSONwriter();
            writer.value(enabled);

            handleVoidCall(makeCall(streams));

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void enableStabilization(final boolean enabled) throws RPCException {

        try {
            final IStreamPair streams = startCall("enableStabilization");
            final JSONWriter writer = streams.getJSONwriter();
            writer.value(enabled);

            handleVoidCall(makeCall(streams));

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void enablePeerStateMaintenance(final boolean enabled) throws RPCException {

        try {
            final IStreamPair streams = startCall("enablePeerStateMaintenance");
            final JSONWriter writer = streams.getJSONwriter();
            writer.value(enabled);

            handleVoidCall(makeCall(streams));

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void notifyFailure(final IChordRemoteReference node) throws RPCException {

        try {
            final IStreamPair streams = startCall("notifyFailure");
            final JSONWriter writer = streams.getJSONwriter();
            marshaller.serializeChordRemoteReference(node, writer);

            handleVoidCall(makeCall(streams));

            finishCall(streams);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public String toStringDetailed() throws RPCException {

        try {
            final IStreamPair streams = startCall("toStringDetailed");

            final JSONReader reader = makeCall(streams);
            final String result = reader.stringValue();

            finishCall(streams);

            return result;
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public String toStringTerse() throws RPCException {

        try {
            final IStreamPair streams = startCall("toStringTerse");

            final JSONReader reader = makeCall(streams);
            final String result = reader.stringValue();

            finishCall(streams);

            return result;
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
            final IStreamPair streams = startCall("toString");

            final JSONReader reader = makeCall(streams);
            final String result = reader.stringValue();

            finishCall(streams);

            return result;
        }
        catch (final Exception e) {
            return "inaccessible";
        }
    }

    @Override
    public int hashCode() {

        try {

            final IStreamPair streams = startCall("hashCode");

            final JSONReader reader = makeCall(streams);
            final int result = reader.intValue();

            finishCall(streams);

            return result;
        }
        catch (final Exception e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error calling remote hashCode()");
            return 0;
        }
    }
}
