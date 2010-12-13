package uk.ac.standrews.cs.stachord.impl;


public class RemoteChordException extends Exception {

    public RemoteChordException(final String message) {

        super(message);
    }

    public RemoteChordException(final Throwable cause) {

        super(cause);
    }
}
