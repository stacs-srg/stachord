package uk.ac.standrews.cs.stachord.test.recovery;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.Observable;
import java.util.Observer;

import uk.ac.standrews.cs.nds.events.Event;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;
import uk.ac.standrews.cs.stachord.interfaces.IChordNode;

public class AddressChangeTest implements Observer {

    private static IChordNode impl;
    private static InetSocketAddress socketAddress;

    /**
     * @param args
     * @throws UnknownHostException 
     * @throws RemoteException 
     */
    public static void main(final String[] args) throws UnknownHostException, RemoteException {

        final AddressChangeTest foo = new AddressChangeTest();

        socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 9091);
        impl = ChordNodeFactory.createLocalNode(socketAddress);
        impl.addObserver(foo);

        System.out.println("Running... at " + impl.getSelfReference().getCachedAddress() + " started at: " + socketAddress);

    }

    @Override
    public void update(final Observable o, final Object arg) {

        System.out.println("Running... at " + impl.getSelfReference().getCachedAddress() + " started at: " + socketAddress);

        System.out.println(arg);

        final Event event = (Event) arg;

        if (event.equals(IChordNode.OWN_ADDRESS_CHANGE_EVENT)) {

            System.out.println("calling getkey on successor");
            try {
                impl.getSuccessor().getRemote().getKey();
            }
            catch (final RemoteException e) {
                // TODO Auto-generated catch block
                System.out.println("error: " + e.getMessage());
            }
            System.out.println("called getkey on successor");
        }
    }
}
