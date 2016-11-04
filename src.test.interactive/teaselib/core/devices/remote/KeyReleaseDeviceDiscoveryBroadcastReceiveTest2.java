/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Citizen Cane
 *
 */
public class KeyReleaseDeviceDiscoveryBroadcastReceiveTest2 {
    private static final Logger logger = LoggerFactory
            .getLogger(KeyReleaseDeviceDiscoveryBroadcastReceiveTest2.class);

    @Test
    public void testAwaitDeviceStartupServicesBroadcastMessage()
            throws Exception {
        logger.info("Awaiting device startup broadscast message:");
        DatagramSocket socket = new DatagramSocket(
                new InetSocketAddress("0.0.0.0", LocalNetworkDevice.Port));
        DatagramPacket p = new DatagramPacket(new byte[1024], 1024);
        socket.receive(p);
        logger.info("Detected device startup!");
        socket.close();
    }

    // TODO cannot bind socket to broadcast address - can only send packets to
    // that address
    // -> bind socket to 0.0.0.0:666 to receive broadcast messages - have a
    // single receive thread
}
