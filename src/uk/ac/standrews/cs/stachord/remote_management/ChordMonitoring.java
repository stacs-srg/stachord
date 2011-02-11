package uk.ac.standrews.cs.stachord.remote_management;

import java.util.HashSet;
import java.util.Set;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.stachord.interfaces.IChordRemoteReference;


public class ChordMonitoring {

    /**
     * Traverses the ring from the given node in the given direction, and returns the length of the cycle containing the given node, or zero if there is no such cycle.
     *
     * @param host_descriptor a ring node
     * @param forwards true if the ring should be traversed via successor pointers, false if it should be traversed via predecessor pointers
     * @return the length of the cycle containing the given node, or zero if the ring node is null or there is no such cycle.
     */
    public static int cycleLengthFrom(final HostDescriptor host_descriptor, final boolean forwards) {
    
        final IChordRemoteReference application_reference = (IChordRemoteReference) host_descriptor.getApplicationReference();
    
        if (application_reference == null) { return 0; }
    
        // Record the nodes that have already been encountered.
        final Set<IChordRemoteReference> nodes_encountered = new HashSet<IChordRemoteReference>();
    
        int cycle_length = 0;
    
        IChordRemoteReference node = application_reference;
    
        while (true) {
    
            cycle_length++;
    
            try {
                node = forwards ? node.getRemote().getSuccessor() : node.getRemote().getPredecessor();
            }
            catch (final RPCException e) {
    
                // Error traversing the ring, so it is broken.
                return 0;
            }
    
            // If the node is null, then the cycle is broken.
            if (node == null) { return 0; }
    
            // If the node is the start node, then a cycle has been found.
            if (node.equals(application_reference)) { return cycle_length; }
    
            // If the node is not the start node and it has already been encountered, then there is a cycle but it doesn't contain the start node.
            if (nodes_encountered.contains(node)) { return 0; }
    
            nodes_encountered.add(node);
        }
    }

}
