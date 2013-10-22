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

package uk.ac.standrews.cs.stachord.remote_management;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ConcurrentScanner;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

class ChordCycleLengthScanner extends ConcurrentScanner {

    public static final String RING_SIZE_PROPERTY_NAME = "ring_size";
    private static final Logger LOGGER = Logger.getLogger(ChordCycleLengthScanner.class.getName());
    private static final Duration CYCLE_LENGTH_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private final AtomicInteger min_cycle_legth;
    private final AtomicInteger old_min_cycle_length;

    public ChordCycleLengthScanner(final Duration min_cycle_time) {

        super(min_cycle_time, CYCLE_LENGTH_CHECK_TIMEOUT, true);
        min_cycle_legth = new AtomicInteger();
        old_min_cycle_length = new AtomicInteger();
    }

    @Override
    protected void scan(final ApplicationNetwork network, final ApplicationDescriptor descriptor) {

        int cycle_length;
        try {
            final IChordRemoteReference reference = descriptor.getApplicationReference();
            cycle_length = ChordMonitoring.cycleLengthFrom(reference, true);
            updateMinCycleLength(cycle_length);
        }
        catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, "interrupted while determining cycle length on " + descriptor.getHost(), e);
        }
    }

    @Override
    protected void afterScan() {

        property_change_support.firePropertyChange(RING_SIZE_PROPERTY_NAME, old_min_cycle_length, min_cycle_legth);
        old_min_cycle_length.set(min_cycle_legth.get());
        super.afterScan();
    }

    private void updateMinCycleLength(final int cycle_length) {

        int current_min_cycle_length;
        int new_min_cycle_legth;
        do {
            current_min_cycle_length = min_cycle_legth.get();
            new_min_cycle_legth = Math.min(current_min_cycle_length, cycle_length);
        } while (min_cycle_legth.compareAndSet(current_min_cycle_length, new_min_cycle_legth));
    }
}
