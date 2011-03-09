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

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.DeserializationException;
import uk.ac.standrews.cs.nds.rpc.Marshaller;
import uk.ac.standrews.cs.nds.rpc.Proxy;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.rpc.json.JSONArray;
import uk.ac.standrews.cs.nds.rpc.json.JSONObject;
import uk.ac.standrews.cs.nds.rpc.json.JSONValue;
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
            return marshaller.deserializeKey((String) makeCall("getKey").getValue());
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
            return marshaller.deserializeInetSocketAddress((String) makeCall("getAddress").getValue());
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
            args.put(marshaller.serializeKey(key));
            return marshaller.deserializeChordRemoteReference((JSONObject) makeCall("lookup", args).getValue());
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
            return marshaller.deserializeChordRemoteReference((JSONObject) makeCall("getSuccessor").getValue());
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
            final JSONValue makeCall = makeCall("getPredecessor");
            final JSONObject value = (JSONObject) makeCall.getValue();

            return marshaller.deserializeChordRemoteReference(value);
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
            args.put(marshaller.serializeChordRemoteReference(potential_predecessor));
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
            args.put(marshaller.serializeChordRemoteReference(node));

            makeCall("join", args);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() throws RPCException {

        try {
            return marshaller.deserializeListChordRemoteReference((JSONArray) makeCall("getSuccessorList"));
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
            return marshaller.deserializeListChordRemoteReference((JSONArray) makeCall("getFingerList"));
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
            args.put(marshaller.serializeKey(key));
            return marshaller.deserializeNextHopResult((JSONObject) makeCall("nextHop", args).getValue());
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
            args.put(marshaller.serializeChordRemoteReference(node));
            makeCall("notifyFailure", args);
        }
        catch (final Exception e) {
            dealWithException(e);
        }
    }

    @Override
    public String toStringDetailed() throws RPCException {

        try {
            return (String) makeCall("toStringDetailed").getValue();
        }
        catch (final Exception e) {
            dealWithException(e);
            return null;
        }
    }

    @Override
    public String toStringTerse() throws RPCException {

        try {
            return (String) makeCall("toStringTerse").getValue();
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
            return (String) makeCall("toString").getValue();
        }
        catch (final Exception e) {
            return "inaccessible";
        }
    }

    @Override
    public int hashCode() {

        try {
            return (Integer) makeCall("hashCode").getValue();
        }
        catch (final Exception e) {
            Diagnostic.trace(DiagnosticLevel.RUN, "error calling remote hashCode()");
            return 0;
        }
    }
}
