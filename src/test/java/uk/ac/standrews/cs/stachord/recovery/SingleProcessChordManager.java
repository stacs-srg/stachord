package uk.ac.standrews.cs.stachord.recovery;

import java.net.InetSocketAddress;

import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.stachord.impl.ChordLocalReference;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class SingleProcessChordManager extends ChordManager {

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final IChordNode node = getNodeFactory().createNode(new InetSocketAddress(host.getAddress(), 0), descriptor.getAttribute(ChordNetwork.PEER_KEY));
        final InetSocketAddress address = node.getAddress();
        final IChordRemoteReference node_reference = bindWithRetry(address);
        attemptJoinAndCleanUpUponFailure(node, node_reference);
        return new ChordLocalReference(node, node_reference);
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {
            final ChordLocalReference reference = descriptor.getApplicationReference();
            if (reference != null) {
                final IChordNode node = reference.getNode();
                node.shutDown();
            }
        }
        finally {
            super.kill(descriptor);
        }
    }

    private void attemptJoinAndCleanUpUponFailure(final IChordNode node, final IChordRemoteReference node_reference) throws Exception {

        try {
            joinWithTimeout(node_reference);
        }
        catch (final Exception e) {
            node.shutDown();
            throw e;
        }
    }
}
