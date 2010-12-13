package uk.ac.standrews.cs.stachord.impl;

import uk.ac.standrews.cs.nds.rpc.RPCException;

public class RemoteChordException extends RPCException {

    public RemoteChordException(final String message) {

        super(message);
    }

    public RemoteChordException(final Throwable cause) {

        super(cause);
    }
}
