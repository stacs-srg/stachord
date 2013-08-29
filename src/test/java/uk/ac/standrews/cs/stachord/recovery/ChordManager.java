package uk.ac.standrews.cs.stachord.recovery;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public abstract class ChordManager extends AbstractApplicationManager {

    static final Duration DEFAULT_JOIN_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    static final Duration DEFAULT_BIND_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    static final Duration DEFAULT_RETRY_DELAY = new Duration(3, TimeUnit.SECONDS);
    private static final Logger LOGGER = Logger.getLogger(ChordManager.class.getName());
    private static final int SEED = 654654;
    private final ChordNodeFactory node_factory;
    private final List<IChordRemoteReference> joined_nodes;
    private final Random random;

    protected ChordManager() {

        node_factory = new ChordNodeFactory();
        joined_nodes = new ArrayList<IChordRemoteReference>();
        random = new Random(SEED);
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        joined_nodes.remove(descriptor.getApplicationReference());
    }

    protected ChordNodeFactory getNodeFactory() {

        return node_factory;
    }

    @Override
    protected void attemptApplicationCall(final ApplicationDescriptor descriptor) throws Exception {

        final IChordRemoteReference reference = descriptor.getApplicationReference();
        try {
            reference.ping();
        }
        catch (final Exception e) {
            joined_nodes.remove(reference);
            throw e;
        }
    }

    protected IChordRemoteReference bindWithRetry(final InetSocketAddress address) throws InterruptedException, ExecutionException, TimeoutException {

        return bindWithRetry(address, DEFAULT_BIND_TIMEOUT, DEFAULT_RETRY_DELAY);
    }

    protected IChordRemoteReference bindWithRetry(final InetSocketAddress address, final Duration timeout, final Duration delay) throws InterruptedException, ExecutionException, TimeoutException {

        return TimeoutExecutorService.awaitCompletion(new Callable<IChordRemoteReference>() {

            @Override
            public IChordRemoteReference call() throws Exception {

                Exception error;
                do {
                    delay.sleep();
                    try {
                        return getNodeFactory().bindToNode(address);
                    }
                    catch (final RuntimeException e) {
                        error = e;
                    }
                    catch (final Exception e) {
                        error = e;
                    }

                }
                while (!Thread.currentThread().isInterrupted() && error != null);
                throw error;
            }
        }, timeout);
    }

    protected void joinWithTimeout(final IChordRemoteReference node_reference) throws InterruptedException, ExecutionException, TimeoutException {

        joinWithTimeout(node_reference, DEFAULT_JOIN_TIMEOUT, DEFAULT_RETRY_DELAY);
    }

    protected void joinWithTimeout(final IChordRemoteReference node_reference, final Duration timeout, final Duration delay) throws InterruptedException, ExecutionException, TimeoutException {

        TimeoutExecutorService.awaitCompletion(new Callable<Void>() {

            @Override
            public Void call() throws Exception {

                boolean succeeded = false;
                do {
                    try {
                        final IChordRemoteReference known_node = getRandomKnownNode(node_reference);
                        node_reference.getRemote().join(known_node);
                        LOGGER.info(node_reference.getCachedKey() + " JOINED " + known_node.getCachedKey());
                        succeeded = true;
                    }
                    catch (final Exception e) {
                        delay.sleep();
                    }
                }
                while (!Thread.currentThread().isInterrupted() && !succeeded);
                if (succeeded) {
                    addToJoinedNodes(node_reference);
                }
                return null; // void task
            }
        }, timeout);
    }

    private synchronized void addToJoinedNodes(final IChordRemoteReference node_reference) {

        if (!joined_nodes.contains(node_reference)) {
            joined_nodes.add(node_reference);
        }
    }

    private synchronized IChordRemoteReference getRandomKnownNode(final IChordRemoteReference joinee) {

        if (joined_nodes.isEmpty()) {
            joined_nodes.add(joinee);
            return joinee;
        }
        else {
            final int candidate_index = random.nextInt(joined_nodes.size());
            return joined_nodes.get(candidate_index);
        }
    }

}
