package uk.ac.standrews.cs.stachord.test.recovery;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class KillableChordRemoteReference implements IChordRemoteReference {

    private final IChordRemoteReference chord_remote_reference;
    private final IChordRemote real_remote;
    private boolean failed = false;
    private final IChordRemote killable_proxy;

    protected IChordRemote getRealRemote() {

        return real_remote;
    }

    private static Map<IChordRemoteReference, KillableChordRemoteReference> map = new HashMap<IChordRemoteReference, KillableChordRemoteReference>();

    public static KillableChordRemoteReference getKillableChordRemoteReference(final IChordRemoteReference chord_remote_reference) {

        if (chord_remote_reference == null) { return null; }

        if (map.containsKey(chord_remote_reference)) { return map.get(chord_remote_reference); }
        final KillableChordRemoteReference killable_reference = new KillableChordRemoteReference(chord_remote_reference);
        map.put(chord_remote_reference, killable_reference);
        return killable_reference;
    }

    private KillableChordRemoteReference(final IChordRemoteReference chord_remote_reference) {

        this.chord_remote_reference = chord_remote_reference;
        real_remote = chord_remote_reference.getRemote();
        killable_proxy = new KillableProxy(this);
    }

    @Override
    public void ping() throws RPCException {

        if (failed) { throw new RPCException("simulated failure"); }
        chord_remote_reference.ping();
    }

    @Override
    public IKey getCachedKey() throws RPCException {

        return chord_remote_reference.getCachedKey();
    }

    @Override
    public InetSocketAddress getCachedAddress() {

        return chord_remote_reference.getCachedAddress();
    }

    @Override
    public IChordRemote getRemote() {

        return killable_proxy;
    }

    public void setFailed() {

        failed = true;
    }

    protected boolean hasFailed() {

        return failed;
    }
}
