/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.InterfaceAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Citizen Cane
 *
 */
public class NetworkDeviceDiscoveryProductionCodeTest {
    private static final Logger logger = LoggerFactory
            .getLogger(NetworkDeviceDiscoveryProductionCodeTest.class);

    static final int Minutes = 2;

    @Test
    public void testAwaitDeviceStartupServicesBroadcastMessage()
            throws Exception {
        logger.info("Awaiting device startup broadscast message:");
        LocalNetworkDeviceDiscoveryBroadcast scanner = new LocalNetworkDeviceDiscoveryBroadcast();
        try {
            List<InterfaceAddress> networks = scanner.networks();
            scanner.updateInterfaceBroadcastListeners(networks);
            final CountDownLatch deviceConnected = new CountDownLatch(1);
            scanner.addRemoteDeviceDiscoveryListener(
                    new RemoteDeviceListener() {
                        @Override
                        public void deviceAdded(String name, String address,
                                String serviceName, String description,
                                String version) {
                            logger.info(name + ":" + serviceName + ", "
                                    + description + ", " + version + "@"
                                    + address);
                            deviceConnected.countDown();
                        }
                    });
            deviceConnected.await();
            logger.info("Device startup message detected");
        } finally {
            scanner.close();
        }
    }
}
