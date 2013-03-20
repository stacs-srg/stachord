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

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.AbstractApplicationManager;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.util.TimeoutExecutorService;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public abstract class ChordManager extends AbstractApplicationManager {

    private static final Logger LOGGER = Logger.getLogger(ChordManager.class.getName());

    private static final int SEED = 654654;
    static final Duration DEFAULT_JOIN_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    static final Duration DEFAULT_BIND_TIMEOUT = new Duration(10, TimeUnit.SECONDS);
    static final Duration DEFAULT_RETRY_DELAY = new Duration(3, TimeUnit.SECONDS);

    private final ChordNodeFactory node_factory;
    private final List<IChordRemoteReference> joined_nodes;
    private final Random random;

    protected ChordManager() {

        node_factory = new ChordNodeFactory();
        joined_nodes = new ArrayList<IChordRemoteReference>();
        random = new Random(SEED);
    }

    protected ChordNodeFactory getNodeFactory() {

        return node_factory;
    }

    @Override
    public void kill(final ApplicationDescriptor descriptor) throws Exception {

        try {
            joined_nodes.remove(descriptor.getApplicationReference());
        }
        finally {
            super.kill(descriptor);
        }
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

                boolean succeeded = false;
                Exception error;
                do {
                    try {
                        delay.sleep();
                        final IChordRemoteReference reference = getNodeFactory().bindToNode(address);
                        succeeded = true;
                        return reference;
                    }
                    catch (final InterruptedException e) {
                        error = e;
                        Thread.currentThread().interrupt();
                    }
                    catch (final Exception e) {
                        error = e;
                        succeeded = false;
                    }
                }
                while (!Thread.currentThread().isInterrupted() && !succeeded);
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
                return null; // void task
            }
        }, DEFAULT_JOIN_TIMEOUT);
    }

    private IChordRemoteReference getRandomKnownNode(final IChordRemoteReference joinee) {

        synchronized (joined_nodes) {
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

}
