package uk.ac.standrews.cs.stachord.recovery;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
import uk.ac.standrews.cs.shabdiz.host.exec.JavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.platform.Platform;
import uk.ac.standrews.cs.shabdiz.util.AttributeKey;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.ProcessUtil;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.servers.NodeServer;

public class MultipleProcessChordManager extends ChordManager {

    private static final Duration PROCESS_START_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final AttributeKey<Process> PEER_PROCESS_KEY = new AttributeKey<Process>();
    private static final AttributeKey<Integer> PEER_PROCESS_PID_KEY = new AttributeKey<Integer>();

    public MultipleProcessChordManager() {

    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();

        // TODO Make process builder explicit
        final JavaProcessBuilder process_builder = new JavaProcessBuilder(NodeServer.class);
        process_builder.addCommandLineArgument("-s:" + descriptor.getAttribute(ChordNetwork.PEER_PORT));
        process_builder.addCommandLineArgument("-x" + descriptor.getAttribute(ChordNetwork.PEER_KEY).toString(Key.DEFAULT_RADIX));
        process_builder.addCurrentJVMClasspath();
        final Process node_process = process_builder.start(host);
        final String address_as_string = ProcessUtil.scanProcessOutput(node_process, NodeServer.CHORD_NODE_LOCAL_ADDRESS_KEY, PROCESS_START_TIMEOUT);
        final String runtime_mx_bean_name = ProcessUtil.scanProcessOutput(node_process, NodeServer.RUNTIME_MX_BEAN_NAME_KEY, PROCESS_START_TIMEOUT);
        final InetSocketAddress address = NetworkUtil.getAddressFromString(address_as_string);
        final Integer pid = ProcessUtil.getPIDFromRuntimeMXBeanName(runtime_mx_bean_name);
        final IChordRemoteReference node_reference = bindWithRetry(new InetSocketAddress(host.getAddress(), address.getPort()));
        descriptor.setAttribute(PEER_PROCESS_KEY, node_process);
        descriptor.setAttribute(PEER_PROCESS_PID_KEY, pid);
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

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {

            final Integer pid = descriptor.getAttribute(PEER_PROCESS_PID_KEY);
            if (pid != null) {
                final Platform platform = descriptor.getHost().getPlatform();
                final String kill_command = Commands.KILL_BY_PROCESS_ID.get(platform, String.valueOf(pid));
                final Process kill = descriptor.getHost().execute(kill_command);
                ProcessUtil.awaitNormalTerminationAndGetOutput(kill);
            }

            final Process process = descriptor.getAttribute(PEER_PROCESS_KEY);
            if (process != null) {
                process.destroy();
            }

        }
        finally {
            super.kill(descriptor);
        }
    }
}
