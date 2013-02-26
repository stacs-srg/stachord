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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.shabdiz.DefaultMadfaceManager;
import uk.ac.standrews.cs.shabdiz.HostDescriptor;
import uk.ac.standrews.cs.shabdiz.scanners.ConcurrentHostScanner;

class ChordCycleLengthScanner extends ConcurrentHostScanner {

    private static final Logger LOGGER = Logger.getLogger(ChordCycleLengthScanner.class.getName());
    private static final Duration CYCLE_LENGTH_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private volatile Integer min_cycle_legth;
    private volatile Integer old_min_cycle_length;
    private final ReentrantLock cycle_length_lock;

    public ChordCycleLengthScanner(final ExecutorService executor, final DefaultMadfaceManager manager, final Duration min_cycle_time) {

        super(executor, manager, min_cycle_time, CYCLE_LENGTH_CHECK_TIMEOUT, "cycle length scanner", true);
        cycle_length_lock = new ReentrantLock();
    }

    @Override
    public String getToggleLabel() {

        return null; // No toggle in user interface required.
    }

    @Override
    public String getName() {

        return "Cycle Length";
    }

    @Override
    protected void check(final HostDescriptor host_descriptor) {

        int cycle_length;
        try {
            cycle_length = ChordMonitoring.cycleLengthFrom(host_descriptor, true);
            updateMinCycleLength(cycle_length);
        }
        catch (final InterruptedException e) {
            LOGGER.log(Level.WARNING, "interrupted while determining cycle length on " + host_descriptor.getHost(), e);
        }
    }

    @Override
    public void cycleFinished() {

        property_change_support.firePropertyChange(ChordManager.RING_SIZE_NAME, old_min_cycle_length, min_cycle_legth);
        old_min_cycle_length = min_cycle_legth;
        super.cycleFinished();
    }

    private void updateMinCycleLength(final int cycle_length) {

        cycle_length_lock.lock();
        try {
            min_cycle_legth = Math.min(min_cycle_legth, cycle_length);
        }
        finally {
            cycle_length_lock.unlock();
        }
    }
}
