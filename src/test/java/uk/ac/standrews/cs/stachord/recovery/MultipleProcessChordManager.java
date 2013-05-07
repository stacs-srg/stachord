package uk.ac.standrews.cs.stachord.recovery;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.JavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.servers.NodeServer;

public class MultipleProcessChordManager extends ChordManager {

    private static final Duration PROCESS_START_TIMEOUT = new Duration(30, TimeUnit.SECONDS);

    public MultipleProcessChordManager() {

    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final ChordNodeDescriptor node_descriptor = (ChordNodeDescriptor) descriptor;
        final JavaProcessBuilder process_builder = new JavaProcessBuilder(NodeServer.class);
        process_builder.addCommandLineArgument("-s:" + node_descriptor.getNodePort());
        process_builder.addCommandLineArgument("-x" + node_descriptor.getNodeKey().toString(Key.DEFAULT_RADIX));
        process_builder.addCurrentJVMClasspath();
        final Process node_process = process_builder.start(host);
        final String address_as_string = ProcessUtil.getValueFromProcessOutput(node_process, NodeServer.CHORD_NODE_LOCAL_ADDRESS_KEY, PROCESS_START_TIMEOUT);
        final InetSocketAddress address = NetworkUtil.getAddressFromString(address_as_string);
        final IChordRemoteReference node_reference = bindWithRetry(new InetSocketAddress(host.getAddress(), address.getPort()));
        attemptJoinAndCleanUpUponFailure(node_process, node_reference);
        return node_reference;
    }

    private void attemptJoinAndCleanUpUponFailure(final Process node_process, final IChordRemoteReference node_reference) throws Exception {

        try {
            joinWithTimeout(node_reference);
        }
        catch (final Exception e) {
            node_process.destroy();
            throw e;
        }
    }
}
