package uk.ac.standrews.cs.stachord.test.recovery;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.impl.NextHopResult;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class KillableProxy implements IChordRemote {

    private final KillableChordRemoteReference killable_remote_reference;
    private final IChordRemote real_remote;

    public KillableProxy(final KillableChordRemoteReference killable_remote_reference) {

        this.killable_remote_reference = killable_remote_reference;
        real_remote = killable_remote_reference.getRealRemote();
    }

    private void checkFailed() throws RPCException {

        if (killable_remote_reference.hasFailed()) { throw new RPCException("simulated failure"); }
    }

    @Override
    public IKey getKey() throws RPCException {

        checkFailed();
        return real_remote.getKey();
    }

    @Override
    public InetSocketAddress getAddress() throws RPCException {

        checkFailed();
        return real_remote.getAddress();
    }

    @Override
    public IChordRemoteReference lookup(final IKey key) throws RPCException {

        checkFailed();
        return KillableChordRemoteReference.getKillableChordRemoteReference(real_remote.lookup(key));
    }

    @Override
    public IChordRemoteReference getSuccessor() throws RPCException {

        checkFailed();
        return KillableChordRemoteReference.getKillableChordRemoteReference(real_remote.getSuccessor());
    }

    @Override
    public IChordRemoteReference getPredecessor() throws RPCException {

        checkFailed();
        return KillableChordRemoteReference.getKillableChordRemoteReference(real_remote.getPredecessor());
    }

    @Override
    public void notify(final IChordRemoteReference potential_predecessor) throws RPCException {

        checkFailed();
        real_remote.notify(KillableChordRemoteReference.getKillableChordRemoteReference(potential_predecessor));
    }

    @Override
    public void join(final IChordRemoteReference node) throws RPCException {

        checkFailed();
        real_remote.join(KillableChordRemoteReference.getKillableChordRemoteReference(node));
    }

    @Override
    public List<IChordRemoteReference> getSuccessorList() throws RPCException {

        checkFailed();
        final List<IChordRemoteReference> successors = new ArrayList<IChordRemoteReference>();
        for (final IChordRemoteReference successor : real_remote.getSuccessorList()) {
            successors.add(KillableChordRemoteReference.getKillableChordRemoteReference(successor));
        }
        return successors;
    }

    @Override
    public List<IChordRemoteReference> getFingerList() throws RPCException {

        checkFailed();
        final List<IChordRemoteReference> fingers = new ArrayList<IChordRemoteReference>();
        for (final IChordRemoteReference finger : real_remote.getFingerList()) {
            fingers.add(KillableChordRemoteReference.getKillableChordRemoteReference(finger));
        }
        return fingers;
    }

    @Override
    public NextHopResult nextHop(final IKey key) throws RPCException {

        checkFailed();
        return real_remote.nextHop(key);
    }

    @Override
    public void enablePredecessorMaintenance(final boolean enabled) throws RPCException {

        checkFailed();
        real_remote.enablePredecessorMaintenance(enabled);
    }

    @Override
    public void enableStabilization(final boolean enabled) throws RPCException {

        checkFailed();
        real_remote.enableStabilization(enabled);
    }

    @Override
    public void enablePeerStateMaintenance(final boolean enabled) throws RPCException {

        checkFailed();
        real_remote.enablePeerStateMaintenance(enabled);
    }

    @Override
    public void notifyFailure(final IChordRemoteReference node) throws RPCException {

        checkFailed();
        real_remote.notifyFailure(KillableChordRemoteReference.getKillableChordRemoteReference(node));
    }

    @Override
    public String toStringDetailed() throws RPCException {

        checkFailed();
        return real_remote.toStringDetailed();
    }

    @Override
    public String toStringTerse() throws RPCException {

        checkFailed();
        return real_remote.toStringTerse();
    }

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
    public int hashCode() {

        return real_remote.hashCode();
    }
}
