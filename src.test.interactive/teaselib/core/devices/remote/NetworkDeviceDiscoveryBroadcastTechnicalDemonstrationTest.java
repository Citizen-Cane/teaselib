/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Citizen Cane
 *
 */
public class NetworkDeviceDiscoveryBroadcastTechnicalDemonstrationTest {
    private static final Logger logger = LoggerFactory
            .getLogger(NetworkDeviceDiscoveryBroadcastTechnicalDemonstrationTest.class);

    @Test
    public void testAwaitDeviceStartupServicesBroadcastMessageViaUDPConnection()
            throws Exception {
        logger.info(
                "Awaiting device startup broadcast message via UDP connection:");
        UDPConnection socket = new UDPConnection(
                new InetSocketAddress("0.0.0.0", LocalNetworkDevice.Port));
        socket.receive();
        logger.info("Device startup detected!");
        socket.close();
    }
}
