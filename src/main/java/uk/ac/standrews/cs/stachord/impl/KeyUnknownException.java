package uk.ac.standrews.cs.stachord.impl;

import uk.ac.standrews.cs.nds.p2p.interfaces.IKey;
import uk.ac.standrews.cs.nds.rpc.RPCException;

/**
 * When the predecessor is needed (such as with {@link ChordNodeImpl#inLocalKeyRange(IKey)}, but the
 * node doesn't have its key.
 *
 * @author Angus Macdonald (angus AT cs.st-andrews.ac.uk)
 */
public class KeyUnknownException extends RPCException {

    private static final long serialVersionUID = 9118195319302052447L;

    public KeyUnknownException(final String message) {

        super(message);
    }
}
