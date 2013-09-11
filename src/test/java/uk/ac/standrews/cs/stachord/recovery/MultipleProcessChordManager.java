package uk.ac.standrews.cs.stachord.recovery;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.util.NetworkUtil;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.host.Host;
import uk.ac.standrews.cs.shabdiz.host.exec.AgentBasedJavaProcessBuilder;
import uk.ac.standrews.cs.shabdiz.host.exec.Bootstrap;
import uk.ac.standrews.cs.shabdiz.host.exec.Commands;
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
    private final AgentBasedJavaProcessBuilder process_builder;

    public MultipleProcessChordManager() {

        process_builder = new AgentBasedJavaProcessBuilder();
        process_builder.addMavenDependency("uk.ac.standrews.cs", "stachord", "2.0-SNAPSHOT");
        process_builder.setMainClass(NodeServer.class);
        process_builder.setDeleteWorkingDirectoryOnExit(true);
    }

    @Override
    public Object deploy(final ApplicationDescriptor descriptor) throws Exception {

        final Host host = descriptor.getHost();
        final Integer port = descriptor.getAttribute(ChordNetwork.PEER_PORT);
        final String node_key_as_string = descriptor.getAttribute(ChordNetwork.PEER_KEY).toString(Key.DEFAULT_RADIX);
        final Process node_process = process_builder.start(host, "-s:" + port, "-x" + node_key_as_string);
        final Properties properties = Bootstrap.readProperties(NodeServer.class, node_process, PROCESS_START_TIMEOUT);
        final Integer pid = Bootstrap.getPIDProperty(properties);
        final int remote_port = getRemotePortFromProperties(properties);
        final IChordRemoteReference node_reference = bindWithRetry(new InetSocketAddress(host.getAddress(), remote_port));
        descriptor.setAttribute(PEER_PROCESS_KEY, node_process);
        descriptor.setAttribute(PEER_PROCESS_PID_KEY, pid);
        attemptJoinAndCleanUpUponFailure(node_process, node_reference);
        return node_reference;
    }

    private Integer getRemotePortFromProperties(final Properties properties) throws UnknownHostException {

        final String port_as_string = properties.getProperty(NodeServer.CHORD_NODE_LOCAL_ADDRESS_KEY);
        return port_as_string != null ? NetworkUtil.getAddressFromString(port_as_string).getPort() : null;
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
