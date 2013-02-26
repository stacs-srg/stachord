/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2010 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland
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

package uk.ac.standrews.cs.stachord.test.recovery;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.p2p.keys.Key;
import uk.ac.standrews.cs.nds.p2p.keys.RingArithmetic;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Diagnostic;
import uk.ac.standrews.cs.nds.util.DiagnosticLevel;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.ErrorHandling;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;
import uk.ac.standrews.cs.shabdiz.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.p2p.network.INetwork;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemote;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;
import uk.ac.standrews.cs.stachord.remote_management.ChordMonitoring;

/**
 * Core Chord test logic. In general, due to asynchrony we can't test much of interest immediately after a given operation.
 * Mostly all we can do is test that some condition eventually holds, by using a test that doesn't complete until the condition does hold.
 *
 * @author Angus Macdonald (angus@cs.st-andrews.ac.uk)
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public final class RecoveryTestLogic {

    /**
     * Prevent instantiation of utility class.
     */
    private RecoveryTestLogic() {

    }

    /**
     * The proportion of the nodes to be killed off in recovery tests.
     */
    public static final double PROPORTION_TO_KILL = 0.2;

    private static final int RANDOM_SEED = 32423545;

    private static final Duration CHECK_WAIT_DELAY = new Duration(3, TimeUnit.SECONDS);

    // -------------------------------------------------------------------------------------------------------

    /**
     * Tests the ability of the given network to recover from the killing off of a proportion ({@link #PROPORTION_TO_KILL}) of its nodes.
     * The test performs the following sequence:
     *
     * <ol>
     * <li>wait for the ring to stabilize</li>
     * <li>disable finger table maintenance on all nodes</li>
     * <li>kill off part of the network</li>
     * <li>wait for routing to be performed correctly by the remaining nodes (which should happen even in the absence of finger table repair)</li>
     * <li>re-enable finger table maintenance</li>
     * <li>wait for the ring to stabilize</li>
     * <li>wait for complete finger tables to be built on all nodes</li>
     * <li>wait for complete successor lists to be built on all nodes</li>
     * <li>wait for routing to be performed correctly by all nodes</li>
     * </ol>
     *
     * @param network a Chord network
     * @param test_timeout the timeout for individual steps of the test, in ms
     * @param ring_creation_start the time at which ring creation was started
     * @throws Exception if the network cannot be shut down
     */
    public static void testRingRecoveryFromNodeFailure(final INetwork network, final Duration test_timeout, final Duration ring_creation_start) throws Exception {

        Duration start_time = printElapsedTime(ring_creation_start);

        final SortedSet<HostDescriptor> nodes = network.getNodes();

        try {
            System.out.println("waiting for stable ring... ");
            waitForStableRing(nodes, test_timeout);
            start_time = printElapsedTime(start_time);

            System.out.println("killing part of network... ");
            killPartOfNetwork(network);
            start_time = printElapsedTime(start_time);

            System.out.println("waiting for stable ring... ");
            waitForStableRing(nodes, test_timeout);
            start_time = printElapsedTime(start_time);

            System.out.println("waiting for complete finger tables... ");
            waitForCompleteFingerTables(nodes, test_timeout);
            start_time = printElapsedTime(start_time);

            System.out.println("waiting for complete successor lists... ");
            waitForCompleteSuccessorLists(nodes, test_timeout);
            start_time = printElapsedTime(start_time);

            System.out.println("waiting for correct routing... ");
            waitForCorrectRouting(nodes, test_timeout);
            start_time = printElapsedTime(start_time);
        }
        catch (final TimeoutException e) {
            throw e;
        }
        finally {

            System.out.println("killing remaining nodes... ");
            network.killAllNodes();
            start_time = printElapsedTime(start_time);

            System.out.println("shutting down network... ");
            network.shutdown();
            printElapsedTime(start_time);
        }
    }

    private static Duration printElapsedTime(final Duration start) {

        final Duration current = Duration.elapsed();
        final Duration elapsed = Duration.elapsed(start);
        System.out.println("done in " + elapsed);

        return current;
    }

    /**
     * Waits for each node in the ring to become stable. See {@link #ringStable(HostDescriptor, int)} for definition of stability.
     *
     * @param nodes a list of Chord nodes
     * @param test_timeout the timeout interval, in ms
     * @throws TimeoutException if the ring does not become stable within the timeout interval
     */
    public static void waitForStableRing(final SortedSet<HostDescriptor> nodes, final Duration test_timeout) throws TimeoutException {

        checkWithTimeout(nodes, new IRingCheck() {

            @Override
            public boolean check(final SortedSet<HostDescriptor> nodes) {

                return ringStable(nodes);
            }

        }, test_timeout);

        Diagnostic.trace(DiagnosticLevel.RUN, "ring is stable");
    }

    /**
     * Waits for each node in the ring to have a complete finger table. See {@link #fingerTableComplete(HostDescriptor)} for definition of completeness.
     *
     * @param nodes a list of Chord nodes
     * @param test_timeout the timeout interval, in ms
     * @throws TimeoutException if the finger tables do not become complete within the timeout interval
     */
    public static void waitForCompleteFingerTables(final SortedSet<HostDescriptor> nodes, final Duration test_timeout) throws TimeoutException {

        checkWithTimeout(nodes, new IRingCheck() {

            @Override
            public boolean check(final SortedSet<HostDescriptor> nodes) {

                return fingerTableComplete(nodes);
            }

        }, test_timeout);

        Diagnostic.trace(DiagnosticLevel.RUN, "finger tables are complete");
    }

    /**
     * Waits for each node in the ring to have a complete successor list. See {@link #successorListComplete(HostDescriptor, int)} for definition of completeness.
     *
     * @param nodes a list of Chord nodes
     * @param test_timeout the timeout interval, in ms
     * @throws TimeoutException if the successor lists do not become complete within the timeout interval
     */
    public static void waitForCompleteSuccessorLists(final SortedSet<HostDescriptor> nodes, final Duration test_timeout) throws TimeoutException {

        checkWithTimeout(nodes, new IRingCheck() {

            @Override
            public boolean check(final SortedSet<HostDescriptor> nodes) {

                return successorListComplete(nodes);
            }

        }, test_timeout);

        Diagnostic.trace(DiagnosticLevel.RUN, "successor lists are consistent");
    }

    /**
     * Waits for each node in the ring to be able to route correctly to every other node. See {@link #routingCorrect(IChordRemoteReference, IChordRemoteReference)} for definition of correctness.
     *
     * @param nodes a list of Chord nodes
     * @param test_timeout the timeout interval, in ms
     * @throws TimeoutException if not all nodes become able to route correctly within the timeout interval
     */
    public static void waitForCorrectRouting(final SortedSet<HostDescriptor> nodes, final Duration test_timeout) throws TimeoutException {

        checkWithTimeout(nodes, new IRingCheck() {

            @Override
            public boolean check(final SortedSet<HostDescriptor> nodes) {

                return routingCorrect(nodes);
            }

        }, test_timeout);

        Diagnostic.trace(DiagnosticLevel.RUN, "routing is correct");
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Tests whether all nodes in are ring are stable. See {@link #ringStable(HostDescriptor, int)} for definition of stability.
     * We define a single-node network as stable. Should really have null as predecessor and self as successor, but don't insist on this because
     * setting self as successor if no running successor can be found when dealing with errors would
     * preclude recovery from transient faults or successor that fails and recovers. See joinUsingFinger() in ChordMaintenanceThread.
     *
     * @param host_descriptors a list of Chord nodes
     * @return true if all nodes are stable
     */
    public static boolean ringStable(final SortedSet<HostDescriptor> host_descriptors) {

        final int network_size = host_descriptors.size();
        if (network_size == 1) { return true; }

        for (final HostDescriptor host_descriptor : host_descriptors) {
            try {
                if (!ringStable(host_descriptor, network_size)) { return false; }
            }
            catch (final InterruptedException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests whether the given node in a ring is stable. The stability criteria are:
     *
     * <ol>
     * <li>the length of the cycle obtained by following successor pointers from the given node is equal to the size of the network</li>
     * <li>the length of the cycle obtained by following predecessor pointers from the given node is equal to the size of the network</li>
     * <li>no errors occur during the test</li>
     * </ol>
     *
     * This is perhaps a bit weak as a test, since it doesn't guarantee that all nodes encountered in the cycle are actually part of this network.
     *
     * @param host_descriptor a Chord node
     * @param network_size the known size of the network
     * @return true if the node is stable.
     * @throws InterruptedException 
     */
    public static boolean ringStable(final HostDescriptor host_descriptor, final int network_size) throws InterruptedException {

        // Check that we see cycles containing the same number of nodes as the network size.
        final int cycle_length_forwards = ChordMonitoring.cycleLengthFrom(host_descriptor, true);
        final int cycle_length_backwards = ChordMonitoring.cycleLengthFrom(host_descriptor, false);

        return cycle_length_forwards == network_size && cycle_length_backwards == network_size;
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Checks that the finger table of the given node is complete. The completeness criteria are:
     *
     * <ol>
     * <li>the ring distance from a node's key to its fingers' keys never decreases going up the table</li>
     * <li>no finger table entry is null</li>
     * <li>no errors occur during the test</li>
     * </ol>
     *
     * @param host_descriptor node to be checked
     * @return true if the finger table is complete
     */
    public static boolean fingerTableComplete(final HostDescriptor host_descriptor) {

        final IChordRemoteReference node = (IChordRemoteReference) host_descriptor.getApplicationReference();
        IChordRemoteReference previous_finger_reference = null;

        // For each finger...
        try {
            int finger_number = 0;

            for (final IChordRemoteReference finger_reference : node.getRemote().getFingerList()) {

                // Check that the finger is not null.
                if (finger_reference == null) {
                    //                    System.out.println(finger_number + " ftc1");
                    return false;
                }

                // Check that the finger is not closer in ring distance than the previous non-null finger.
                // Treat self-reference as the full ring distance, so ignore case where finger points to this node.

                final IKey node_key = node.getCachedKey();
                final IKey finger_key = finger_reference.getCachedKey();

                if (previous_finger_reference != null && !finger_key.equals(node_key)) {

                    if (RingArithmetic.ringDistanceFurther(node_key, previous_finger_reference.getCachedKey(), finger_key)) {
                        //                        System.out.println(finger_number + " ftc2");
                        return false;
                    }
                }

                previous_finger_reference = finger_reference;
                finger_number++;
            }
        }
        catch (final RPCException e) {
            //            System.out.println("ftc3");
            return false;
        }

        //        System.out.println("ftc4");
        return true;
    }

    /**
     * Tests whether all nodes in the ring have complete successor lists. See {@link #successorListComplete(HostDescriptor, int)} for definition of completeness.
     * Returns true for a single-node network, since the node may have an external non-functioning successor and a null predecessor, hence it can't route and
     * can't fix its fingers. See {@link #ringStable(SortedSet)} for rationale for allowing this.
     * 
     * @param host_descriptors a list of Chord nodes
     * @return true if all nodes have complete successor lists
     */
    public static boolean successorListComplete(final SortedSet<HostDescriptor> host_descriptors) {

        final int network_size = host_descriptors.size();
        if (network_size == 1) { return true; }

        for (final HostDescriptor host_descriptor : host_descriptors) {
            if (!successorListComplete(host_descriptor, network_size)) { return false; }
        }

        return true;
    }

    /**
     * Checks that the successor list of the given node is complete. The completeness criteria are:
     *
     * <ol>
     * <li>the length of the list is {@link IChordNode#MAX_SUCCESSOR_LIST_SIZE} or one less than the size of the ring, whichever is less</li>
     * <li>the entries in the list follow ring successor order</li>
     * <li>no errors occur during the test</li>
     * </ol>
     *
     * @param host_descriptor node to be checked
     * @param network_size the known size of the network
     * @return true if the successor list is complete
     */
    public static boolean successorListComplete(final HostDescriptor host_descriptor, final int network_size) {

        final IChordRemoteReference application_reference = (IChordRemoteReference) host_descriptor.getApplicationReference();
        final IChordRemote node = application_reference.getRemote();

        List<IChordRemoteReference> successor_list;
        try {
            successor_list = node.getSuccessorList();

            // The length of the successor lists should be MIN(max_successor_list_length, number_of_nodes - 1).
            if (successor_list.size() != Math.min(IChordNode.MAX_SUCCESSOR_LIST_SIZE, network_size - 1)) { return false; }

            // Check that the successors follow the node round the ring.
            IChordRemoteReference ring_node = node.getSuccessor();

            for (final IChordRemoteReference successor_list_node : successor_list) {

                if (!successor_list_node.equals(ring_node)) { return false; }
                ring_node = ring_node.getRemote().getSuccessor();
            }
        }
        catch (final RPCException e) {
            return false;
        }

        return true;
    }

    /**
     * Tests whether routing works correctly between all pairs of nodes. See {@link #routingCorrect(IChordRemoteReference, IChordRemoteReference)} for definition of correctness.
     * Returns true for a single-node network, since the node may have an external non-functioning successor and a null predecessor, hence it can't route. See {@link #ringStable(SortedSet)} for rationale for allowing this.
     *
     * @param host_descriptors a list of Chord nodes
     * @return true if routing works correctly between all pairs of nodes
     */
    public static boolean routingCorrect(final SortedSet<HostDescriptor> host_descriptors) {

        if (host_descriptors.size() == 1) { return true; }

        for (final HostDescriptor host_descriptor1 : host_descriptors) {
            for (final HostDescriptor host_descriptor2 : host_descriptors) {

                final IChordRemoteReference node1 = (IChordRemoteReference) host_descriptor1.getApplicationReference();
                final IChordRemoteReference node2 = (IChordRemoteReference) host_descriptor2.getApplicationReference();

                if (!routingCorrect(node1, node2)) { return false; }
            }
        }
        return true;
    }

    /**
     * Checks that routing works correctly from one given node to another. The correctness criteria are:
     *
     * <ol>
     * <li>routing to the key one less than the target's key should result in the target, unless the target has a predecessor with a key one less than it</li>
     * <li>routing to the target's own key should result in the target</li>
     * <li>routing to the key one more than the target's key should result in the target's successor</li>
     * </ol>
     *
     * @param source the node whose routing is being tested
     * @param target the node to which routing is being tested
     * @return true if routing works correctly
     */
    public static boolean routingCorrect(final IChordRemoteReference source, final IChordRemoteReference target) {

        try {
            return routingToSmallerKeyCorrect(source, target) && routingToSameKeyCorrect(source, target) && routingToLargerKeyCorrect(source, target);
        }
        catch (final RPCException e) {
            return false;
        }
    }

    /**
     * Prints a representation of a network.
     * @param nodes the network
     */
    public static void dumpState(final SortedSet<HostDescriptor> nodes) {

        for (final HostDescriptor machine_descriptor : nodes) {

            System.out.println(machine_descriptor);

            try {
                final IChordRemoteReference application_reference = (IChordRemoteReference) machine_descriptor.getApplicationReference();
                System.out.println(application_reference.getRemote().toStringDetailed());
            }
            catch (final RPCException e) {
                System.out.println("application inaccessible");
            }
            System.out.println();
        }

        System.out.println("\n>>>>>>>>>>>>>>>> End of state\n");
    }

    // -------------------------------------------------------------------------------------------------------

    /**
     * Tests whether all nodes in the ring have complete finger tables. See {@link #fingerTableComplete(HostDescriptor)} for definition of completeness.
     * Returns true for a single-node network, since the node may have an external non-functioning successor and a null predecessor, hence it can't route and
     * can't fix its fingers. See {@link #ringStable(List)} for rationale for allowing this.
     * 
     * @param host_descriptors a list of Chord nodes
     * @return true if all nodes have complete finger tables
     */
    private static boolean fingerTableComplete(final SortedSet<HostDescriptor> host_descriptors) {

        if (host_descriptors.size() == 1) { return true; }

        for (final HostDescriptor host_descriptor : host_descriptors) {
            if (!fingerTableComplete(host_descriptor)) { return false; }
        }

        return true;
    }

    private static boolean routingToSmallerKeyCorrect(final IChordRemoteReference source, final IChordRemoteReference target) throws RPCException {

        // Check that a slightly smaller key than the target's key routes to the target, except
        // in the pathological case where the target has a predecessor with a key one less than it.
        final IChordRemoteReference predecessor_of_target = target.getRemote().getPredecessor();

        final Key one_before_key = new Key(target.getCachedKey().keyValue().subtract(BigInteger.ONE));
        final boolean pathological = predecessor_of_target != null && predecessor_of_target.getCachedKey().equals(one_before_key);

        final IChordRemote result_for_smaller_key = lookup(source, one_before_key);

        return !pathological && result_for_smaller_key.getKey().equals(target.getCachedKey()) || pathological && result_for_smaller_key.getKey().equals(predecessor_of_target.getCachedKey());
    }

    private static boolean routingToSameKeyCorrect(final IChordRemoteReference source, final IChordRemoteReference target) throws RPCException {

        // Check that the target's own key routes to the target.
        final IChordRemote result_for_same_key = lookup(source, target.getCachedKey());
        return target.getCachedKey().equals(result_for_same_key.getKey());
    }

    private static boolean routingToLargerKeyCorrect(final IChordRemoteReference source, final IChordRemoteReference target) throws RPCException {

        // Check that a slightly larger key than the node's key routes to the node's successor.
        final IChordRemoteReference successor_of_target = target.getRemote().getSuccessor();

        final Key one_after_target_key = new Key(target.getCachedKey().keyValue().add(BigInteger.ONE));
        final IChordRemote result_for_larger_key = lookup(source, one_after_target_key);

        return result_for_larger_key.getKey().equals(successor_of_target.getCachedKey());
    }

    private static IChordRemote lookup(final IChordRemoteReference source, final IKey key) throws RPCException {

        return source.getRemote().lookup(key).getRemote();
    }

    private static void killPartOfNetwork(final INetwork network) {

        final SortedSet<HostDescriptor> nodes = network.getNodes();
        final int network_size = nodes.size();

        // No point in killing off the only member of the network and expecting it to recover.
        if (network_size > 1) {

            final int number_to_kill = Math.max(1, (int) (PROPORTION_TO_KILL * network_size));

            // The list of victim indices may contain duplicates since each victim is removed from the network immediately after being killed.
            final List<Integer> victim_indices = pickRandomIndices(number_to_kill, network_size);

            for (final int victim_index : victim_indices) {

                try {
                    final HostDescriptor victim = getElement(nodes, victim_index);
                    network.killNode(victim);
                }
                catch (final Exception e) {
                    ErrorHandling.error(e, "error killing node: " + e.getMessage());
                }
            }

            assertThat(nodes.size(), is(equalTo(network_size - number_to_kill)));
        }
    }

    private static HostDescriptor getElement(final SortedSet<HostDescriptor> nodes, final int index) {

        HostDescriptor element = null;

        // Need to do this to access set element with particular index.
        int host_index = 0;
        for (final HostDescriptor host_descriptor : nodes) {
            if (host_index == index) {
                element = host_descriptor;
                break;
            }
            host_index++;
        }
        return element;
    }

    /**
     * Returns a randomly selected list of integers in a progressively reducing range. The first element in the list
     * is selected from the range 0 to range-1 inclusive; the next from 0 to range-2; and so on.
     *
     * @param number_to_select the number of integers to be returned
     * @param initial_range the initial range to select from
     * @return a list of integers from the specified range
     */
    private static List<Integer> pickRandomIndices(final int number_to_select, final int initial_range) {

        final List<Integer> indices = new ArrayList<Integer>();
        final Random random = new Random(RANDOM_SEED);

        for (int i = 0; i < number_to_select; i++) {
            // Reduce the range each time, since nodes will be successfully removed from the set.
            indices.add(random.nextInt(initial_range - i));
        }

        return indices;
    }

    private static void checkWithTimeout(final SortedSet<HostDescriptor> nodes, final IRingCheck checker, final Duration test_timeout) throws TimeoutException {

        final TimeoutExecutor timeout_executor = TimeoutExecutor.makeTimeoutExecutor(1, test_timeout, true, true, "Chord recovery executor");

        boolean timed_out = false;
        final DiagnosticLevel previous_level = Diagnostic.getLevel();

        try {
            while (!Thread.currentThread().isInterrupted()) {

                try {
                    final boolean check_succeeded = timeout_executor.executeWithTimeout(new Callable<Boolean>() {

                        @Override
                        public Boolean call() throws Exception {

                            return checker.check(nodes);
                        }
                    });

                    if (check_succeeded) {
                        if (timed_out) {
                            System.out.println("\n>>>>>>>>>>>>>>>> Succeeded on last check\n");
                            Diagnostic.setLevel(previous_level);
                        }
                        return;
                    }

                    CHECK_WAIT_DELAY.sleep();
                }
                catch (final TimeoutException e) {

                    if (timed_out) {
                        System.out.println("\n>>>>>>>>>>>>>>>> Test timed out: dumping state\n");
                        dumpState(nodes);
                        throw e;
                    }

                    System.out.println("\n>>>>>>>>>>>>>>>> Potential timeout: executing one more check with full diagnostics\n");
                    Diagnostic.setLevel(DiagnosticLevel.FULL);
                    timed_out = true;
                }
                catch (final Exception e) {
                    throw new TimeoutException("unexpected exception: " + e.getMessage());
                }
            }
        }
        finally {
            timeout_executor.shutdown();
        }
    }

    private interface IRingCheck {

        boolean check(SortedSet<HostDescriptor> nodes);
    }
}
