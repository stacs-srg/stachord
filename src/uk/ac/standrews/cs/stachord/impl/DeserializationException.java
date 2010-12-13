package uk.ac.standrews.cs.stachord.impl;

public class DeserializationException extends Exception {

    public DeserializationException(final Throwable e) {

        super(e);
    }

    public DeserializationException(final String message) {

        super(message);
    }
}
