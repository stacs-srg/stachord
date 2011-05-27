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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.interfaces.IAttributesCallback;
import uk.ac.standrews.cs.nds.madface.interfaces.ISingleHostScanner;
import uk.ac.standrews.cs.nds.madface.scanners.Scanner;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.nds.util.TimeoutExecutor;

/**
 * The Cycle length scanenr.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class ChordCycleLengthScanner extends Scanner implements ISingleHostScanner {

    private static final Duration CYCLE_LENGTH_CHECK_TIMEOUT = new Duration(30, TimeUnit.SECONDS);
    private static final int CYCLE_LENGTH_CHECK_THREADS = 10;

    @Override
    public TimeoutExecutor makeExecutor() {

        return new TimeoutExecutor(CYCLE_LENGTH_CHECK_THREADS, CYCLE_LENGTH_CHECK_TIMEOUT, false, "cycle length scanner");
    }

    @Override
    public String getAttributeName() {

        return ChordManager.RING_SIZE_NAME;
    }

    @Override
    public String getToggleLabel() {

        // No toggle in user interface required.
        return null;
    }

    @Override
    public String getName() {

        return "Cycle Length";
    }

    @Override
    public void check(final HostDescriptor host_descriptor, final Set<IAttributesCallback> attribute_callbacks) throws InterruptedException {

        final int cycle_length = ChordMonitoring.cycleLengthFrom(host_descriptor, true);
        final String cycle_length_string = cycle_length > 0 ? String.valueOf(cycle_length) : "-";
        final Map<String, String> attribute_map = host_descriptor.getAttributes();

        final boolean attributes_changed = !attribute_map.containsKey(getAttributeName()) || !attribute_map.get(getAttributeName()).equals(cycle_length_string);

        attribute_map.put(getAttributeName(), cycle_length_string);

        if (attributes_changed) {
            for (final IAttributesCallback callback : attribute_callbacks) {
                callback.attributesChange(host_descriptor);
            }
        }
    }

    @Override
    public boolean enabledByDefault() {

        return true;
    }
}
