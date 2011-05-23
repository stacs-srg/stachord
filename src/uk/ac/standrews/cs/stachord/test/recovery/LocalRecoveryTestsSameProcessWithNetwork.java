package uk.ac.standrews.cs.stachord.test.recovery;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
import org.junit.Test;

import uk.ac.standrews.cs.nds.madface.HostDescriptor;
import uk.ac.standrews.cs.nds.madface.exceptions.UnknownPlatformException;
import uk.ac.standrews.cs.nds.p2p.network.INetwork;
import uk.ac.standrews.cs.nds.p2p.network.InvalidServerClassException;
import uk.ac.standrews.cs.nds.p2p.network.KeyDistribution;
import uk.ac.standrews.cs.nds.registry.AlreadyBoundException;
import uk.ac.standrews.cs.nds.registry.RegistryUnavailableException;
import uk.ac.standrews.cs.nds.rpc.RPCException;
import uk.ac.standrews.cs.nds.util.Duration;
import uk.ac.standrews.cs.stachord.impl.ChordNodeFactory;

import com.mindbright.ssh2.SSH2Exception;

public class LocalRecoveryTestsSameProcessWithNetwork extends LocalRecoveryTestBase {

    private static final int[] RING_SIZES = {1, 2, 3, 4, 5, 10, 20};

    //    private static final int[] RING_SIZES = {1, 2, 3};

    @Override
    protected INetwork getTestNetwork(final int ring_size, final KeyDistribution network_type) throws Exception {

        return new LocalChordNetworkSingleProcess(ring_size, network_type);
    }

    @Override
    protected int[] getRingSizes() {

        return RING_SIZES;
    }

    @Test
    @Ignore
    public void runSingleNode() throws UnknownHostException, IOException, RPCException, AlreadyBoundException, RegistryUnavailableException, SSH2Exception, TimeoutException, UnknownPlatformException, InvalidServerClassException, InterruptedException {

        final HostDescriptor hd = new HostDescriptor().port(50000);
        hd.deployInLocalProcess(true);

        new ChordNodeFactory().createNode(hd);

        new Duration(2, TimeUnit.MINUTES).sleep();
    }
}
