package uk.ac.standrews.cs.stachord.impl;


public class RemoteException extends Exception {

    public RemoteException(final String message) {

        super(message);
    }

    public RemoteException(final Throwable cause) {

        super(cause);
    }
}
