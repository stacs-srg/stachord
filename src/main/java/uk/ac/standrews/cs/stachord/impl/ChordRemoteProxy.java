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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.json.JSONWriter;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.stream.Connection;
import uk.ac.standrews.cs.nds.rpc.stream.JSONReader;
import uk.ac.standrews.cs.nds.rpc.stream.Marshaller;
import uk.ac.standrews.cs.nds.rpc.stream.StreamProxy;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

/**
 * Proxy for remotely accessible Chord node.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class ChordRemoteProxy extends StreamProxy implements IChordRemote {

    private static final Map<InetSocketAddress, ChordRemoteProxy> PROXY_MAP;

    static {
        PROXY_MAP = new Hashtable<InetSocketAddress, ChordRemoteProxy>(); // Hashtable is used since it only permits non-null keys and values
    }

    private final ChordRemoteMarshaller marshaller;

    // -------------------------------------------------------------------------------------------------------

    private ChordRemoteProxy(final InetSocketAddress node_address) {

        super(node_address);
        marshaller = new ChordRemoteMarshaller();
    }

    // -------------------------------------------------------------------------------------------------------

    static synchronized ChordRemoteProxy getProxy(final InetSocketAddress proxy_address) {

        final ChordRemoteProxy proxy;
        if (PROXY_MAP.containsKey(proxy_address)) { // Throws NPE if the given proxy address is null
            proxy = PROXY_MAP.get(proxy_address);
        }
        else {
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
            final Connection connection = (Connection) startCall("getKey");

            final JSONReader reader = makeCall(connection);
            final IKey result = marshaller.deserializeKey(reader);

            finishCall(connection);

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

        return super.node_address;
    }

    @Override
    public IChordRemoteReference lookup(final IKey key) throws RPCException {

        try {
            final Connection connection = (Connection) startCall("lookup");

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeKey(key, writer);

            final JSONReader reader = makeCall(connection);
            final IChordRemoteReference result = marshaller.deserializeChordRemoteReference(reader);

            finishCall(connection);

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
            final Connection connection = (Connection) startCall("getSuccessor");

            final JSONReader reader = makeCall(connection);
            final IChordRemoteReference result = marshaller.deserializeChordRemoteReference(reader);

            finishCall(connection);

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
            final Connection connection = (Connection) startCall("getPredecessor");

            final JSONReader reader = makeCall(connection);

            final IChordRemoteReference result = marshaller.deserializeChordRemoteReference(reader);

            finishCall(connection);

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
            final Connection connection = (Connection) startCall("notify");

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeChordRemoteReference(potential_predecessor, writer);

            makeVoidCall(connection);

            finishCall(connection);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void join(final IChordRemoteReference node) throws RPCException {

        try {
            final Connection connection = (Connection) startCall("join");

            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeChordRemoteReference(node, writer);

            makeVoidCall(connection);

            finishCall(connection);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() throws RPCException {

        try {
            final Connection connection = (Connection) startCall("getSuccessorList");

            final JSONReader reader = makeCall(connection);
            final List<IChordRemoteReference> result = marshaller.deserializeListChordRemoteReference(reader);

            finishCall(connection);

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
            final Connection connection = (Connection) startCall("getFingerList");

            final JSONReader reader = makeCall(connection);
            final List<IChordRemoteReference> result = marshaller.deserializeListChordRemoteReference(reader);

            finishCall(connection);

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
            final Connection connection = (Connection) startCall("nextHop");
            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeKey(key, writer);

            final JSONReader reader = makeCall(connection);
            final NextHopResult result = marshaller.deserializeNextHopResult(reader);

            finishCall(connection);

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
            final Connection connection = (Connection) startCall("enablePredecessorMaintenance");
            final JSONWriter writer = connection.getJSONwriter();
            writer.value(enabled);

            makeVoidCall(connection);

            finishCall(connection);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void enableStabilization(final boolean enabled) throws RPCException {

        try {
            final Connection connection = (Connection) startCall("enableStabilization");
            final JSONWriter writer = connection.getJSONwriter();
            writer.value(enabled);

            makeVoidCall(connection);

            finishCall(connection);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void enablePeerStateMaintenance(final boolean enabled) throws RPCException {

        try {
            final Connection connection = (Connection) startCall("enablePeerStateMaintenance");
            final JSONWriter writer = connection.getJSONwriter();
            writer.value(enabled);

            makeVoidCall(connection);

            finishCall(connection);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public void notifyFailure(final IChordRemoteReference node) throws RPCException {

        try {
            final Connection connection = (Connection) startCall("notifyFailure");
            final JSONWriter writer = connection.getJSONwriter();
            marshaller.serializeChordRemoteReference(node, writer);

            makeVoidCall(connection);

            finishCall(connection);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public String toStringDetailed() throws RPCException {

        try {
            final Connection connection = (Connection) startCall("toStringDetailed");

            final JSONReader reader = makeCall(connection);
            final String result = reader.stringValue();

            finishCall(connection);

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
            final Connection connection = (Connection) startCall("toStringTerse");

            final JSONReader reader = makeCall(connection);
            final String result = reader.stringValue();

            finishCall(connection);

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
            final Connection connection = (Connection) startCall("toString");

            final JSONReader reader = makeCall(connection);
            final String result = reader.stringValue();

            finishCall(connection);

            return result;
        }
        catch (final Exception e) {
            return "inaccessible";
        }
    }

    @Override
    public int hashCode() {

        try {
            final Connection connection = (Connection) startCall("hashCode");

            final JSONReader reader = makeCall(connection);
            final int result = reader.intValue();

            finishCall(connection);

            return result;
        }
        catch (final Exception e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error calling remote hashCode()");
            return 0;
        }
    }
}
