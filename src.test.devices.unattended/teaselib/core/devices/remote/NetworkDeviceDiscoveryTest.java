/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.devices.Devices;

/**
 * @author Citizen Cane
 *
 */
public class NetworkDeviceDiscoveryTest {
    static final Logger logger = LoggerFactory.getLogger(NetworkDeviceDiscoveryTest.class);

    static final int Minutes = 2;

    @Test
    public void testBroadcastConnectProcedure() throws Exception {
        logger.info("Manual device discovery broadcast:");
        Map<InetAddress, RemoteDeviceMessage> devices = new HashMap<>();
        LocalNetworkDeviceDiscoveryBroadcast localNetworkDeviceDiscoveryBroadcast = new LocalNetworkDeviceDiscoveryBroadcast();
        try {
            for (InterfaceAddress interfaceAddress : localNetworkDeviceDiscoveryBroadcast.networks()) {
                logger.info("Sending broadcast message to {}", interfaceAddress.getBroadcast());

                UDPConnection connection = new UDPConnection(interfaceAddress.getBroadcast(), 666);
                try {
                    connection.send(new UDPMessage(RemoteDevice.Id).toByteArray());
                    while (true) {
                        try {
                            byte[] received = connection.receive(1000);
                            UDPMessage udpMessage = new UDPMessage(received);
                            RemoteDeviceMessage device = udpMessage.message;
                            if ("services".equals(device.command)) {
                                devices.put(InetAddress.getByName(device.parameters.get(1)), device);
                            }
                        } catch (SocketTimeoutException e) {
                            break;
                        }
                    }
                } finally {
                    connection.close();
                }
            }
        } finally {
            localNetworkDeviceDiscoveryBroadcast.close();
        }
        for (Map.Entry<InetAddress, RemoteDeviceMessage> device : devices.entrySet()) {
            logger.info(device.getKey().toString() + " -> " + device.getValue());
        }
    }

    @Test
    public void testLocalNetworkDeviceBroadcastDiscovery() throws Exception {
        logger.info("Local network device discovery broadcast):");
        LocalNetworkDeviceDiscoveryBroadcast scanner = new LocalNetworkDeviceDiscoveryBroadcast();
        try {
            collectFoundDevices(scanner);
        } finally {
            scanner.close();
        }
    }

    private static void collectFoundDevices(LocalNetworkDeviceDiscovery scanner) throws InterruptedException {
        scanner.addRemoteDeviceDiscoveryListener(new RemoteDeviceListener() {
            @Override
            public void deviceAdded(String name, String address, String serviceName, String description,
                    String version) {
                logger.info(name + ":" + serviceName + ", " + description + ", " + version + "@" + address);
            }
        });
        scanner.searchDevices();
        Thread.sleep(2000);
    }

    @Test
    public void testDeviceClass() throws Exception {
        Configuration config = DebugSetup.getConfigurationWithRemoteDeviceAccess();
        Devices devices = new Devices(config);

        logger.info("Device factory network scan (uses broadcast device discovery):");

        for (String devicePath : devices.get(LocalNetworkDevice.class).getDevicePaths()) {
            logger.info(devicePath);
        }
    }
}
