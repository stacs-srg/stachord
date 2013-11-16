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

import java.beans.PropertyChangeListener;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.standrews.cs.shabdiz.ApplicationDescriptor;
import uk.ac.standrews.cs.shabdiz.ApplicationNetwork;
import uk.ac.standrews.cs.shabdiz.ConcurrentScanner;
import uk.ac.standrews.cs.shabdiz.util.Duration;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;

public class ChordRingSizeScanner extends ConcurrentScanner {

    private static final String RING_SIZE_PROPERTY_NAME = "ring_size";
    private static final Logger LOGGER = Logger.getLogger(ChordRingSizeScanner.class.getName());
    private static final Duration DEFAULT_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private final AtomicInteger min_ring_size;
    private final AtomicInteger old_min_ring_size;

    public ChordRingSizeScanner(final Duration interval) {

        this(interval, DEFAULT_TIMEOUT);
    }

    public ChordRingSizeScanner(final Duration interval, final Duration timeout) {

        super(interval, timeout, false);
        min_ring_size = new AtomicInteger();
        old_min_ring_size = new AtomicInteger();
    }

    public void addRingSizeChangeListener(final PropertyChangeListener listener) {

        addPropertyChangeListener(RING_SIZE_PROPERTY_NAME, listener);
    }

    public void removeRingSizeChangeListener(final PropertyChangeListener listener) {

        removePropertyChangeListener(RING_SIZE_PROPERTY_NAME, listener);
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

        property_change_support.firePropertyChange(RING_SIZE_PROPERTY_NAME, old_min_ring_size, min_ring_size);
        old_min_ring_size.set(min_ring_size.get());
        super.afterScan();
    }

    private void updateMinCycleLength(final int cycle_length) {

        int current_min_ring_size;
        int new_min_ring_size;
        do {
            current_min_ring_size = min_ring_size.get();
            new_min_ring_size = Math.min(current_min_ring_size, cycle_length);
        }
        while (min_ring_size.compareAndSet(current_min_ring_size, new_min_ring_size));
    }
}
