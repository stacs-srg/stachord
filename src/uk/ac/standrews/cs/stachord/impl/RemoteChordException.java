/***************************************************************************
 *                                                                         *
 * stachord Library                                                        *
 * Copyright (C) 2004-2011 Distributed Systems Architecture Research Group *
 * University of St Andrews, Scotland                                      *
 * http://beast.cs.st-andrews.ac.uk/                                 *
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
package uk.ac.standrews.cs.stachord.impl;

import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * A Chord-specific remote exception.
 *
 * @author Graham Kirby (graham.kirby@st-andrews.ac.uk)
 */
public class RemoteChordException extends RPCException {

    private static final long serialVersionUID = -7674360342545415238L;

    /**
     * Creates an exception.
     *
     * @param message the message
     */
    public RemoteChordException(final String message) {

        super(message);
    }

    /**
     * Creates an exception.
     *
     * @param cause the cause
     */
    public RemoteChordException(final Throwable cause) {

        super(cause);
    }
}
