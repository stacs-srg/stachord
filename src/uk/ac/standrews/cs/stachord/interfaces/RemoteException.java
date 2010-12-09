package uk.ac.standrews.cs.stachord.interfaces;

import java.io.IOException;

public class RemoteException extends Exception {

    public RemoteException(final IOException e) {

        super(e);
    }

}
