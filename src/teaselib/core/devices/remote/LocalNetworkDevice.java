/**
 * 
 */
package teaselib.core.devices.remote;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.core.devices.DeviceCache;
import teaselib.core.devices.DeviceFactory;

/**
 * @author Citizen-Cane
 *
 */
public class LocalNetworkDevice implements RemoteDevice {
    private static final Logger logger = LoggerFactory
            .getLogger(LocalNetworkDevice.class);

    private static final String DeviceClassName = "LocalNetworkDevice";

    private static final int Port = 666;

    public static final DeviceFactory<RemoteDevice> Factory = new DeviceFactory<RemoteDevice>(
            DeviceClassName) {
        @Override
        public List<String> enumerateDevicePaths(
                Map<String, RemoteDevice> deviceCache)
                throws InterruptedException {
            final ExecutorService es = Executors.newFixedThreadPool(256);
            final List<Future<List<LocalNetworkDevice>>> futures = new ArrayList<Future<List<LocalNetworkDevice>>>();
            try {
                List<Subnet> subnets = enumerateNetworks();
                for (Subnet subnet : subnets) {
                    logger.info("Scanning " + subnet.toString());
                    for (final InetAddress ip : subnet) {
                        futures.add(es.submit(
                                new Callable<List<LocalNetworkDevice>>() {
                                    @Override
                                    public List<LocalNetworkDevice> call()
                                            throws Exception {
                                        UDPConnection udpClient = new UDPConnection(
                                                ip, Port);
                                        return getServices(udpClient,
                                                new UDPMessage(udpClient
                                                        .sendAndReceive(
                                                                new UDPMessage(
                                                                        RemoteDevice.Id)
                                                                                .toByteArray(),
                                                                1000)));
                                    }

                                    private List<LocalNetworkDevice> getServices(
                                            UDPConnection connection,
                                            UDPMessage status)
                                            throws SocketException {
                                        int i = 0;
                                        String name = status.message.parameters
                                                .get(i++);
                                        int serviceCount = Integer.parseInt(
                                                status.message.parameters
                                                        .get(i++));
                                        List<LocalNetworkDevice> devices = new ArrayList<LocalNetworkDevice>(
                                                (status.message.parameters
                                                        .size() - i) / 2);
                                        for (int j = 0; j < serviceCount; j++) {
                                            String serviceName = status.message.parameters
                                                    .get(i++);
                                            String description = status.message.parameters
                                                    .get(i++);
                                            String version = status.message.parameters
                                                    .get(i++);
                                            devices.add((new LocalNetworkDevice(
                                                    name, connection,
                                                    serviceName, description,
                                                    version)));
                                            if (serviceCount == 1) {
                                                break;
                                            } else {
                                                connection = new UDPConnection(
                                                        connection.getAddress(),
                                                        connection.getPort());
                                                continue;
                                            }
                                        }
                                        return devices;
                                    }
                                }));
                    }
                }
            } catch (SocketException e) {
                logger.error(e.getMessage(), e);
            }
            es.shutdown();
            for (final Future<List<LocalNetworkDevice>> f : futures) {
                try {
                    List<LocalNetworkDevice> detectedDevices = f.get();
                    for (LocalNetworkDevice device : detectedDevices) {
                        deviceCache.put(device.getDevicePath(), device);
                    }
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof SocketTimeoutException) {
                        // Expected
                    } else {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
            return new ArrayList<String>(deviceCache.keySet());
        }

        @Override
        public RemoteDevice createDevice(String deviceName) {
            throw new UnsupportedOperationException();
        }

    };

    public static List<Subnet> enumerateNetworks() throws SocketException {
        List<Subnet> subnets = new Vector<Subnet>();
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                .getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(networkInterfaces)) {
            if (netint.isUp() && !netint.isVirtual() && !netint.isLoopback()) {
                for (InterfaceAddress ifa : netint.getInterfaceAddresses()) {
                    if (ifa.getAddress() instanceof Inet4Address) {
                        subnets.add(new Subnet(ifa));
                    }
                }
            }
        }
        return subnets;
    }

    private final String name;
    private final String serviceName;
    private final String description;
    private final String version;

    private UDPConnection connection;

    // TODO rescan network, reconnect to same device at different address

    LocalNetworkDevice(String name, UDPConnection connection,
            String serviceName, String description, String version) {
        this.name = name;
        this.connection = connection;
        this.serviceName = serviceName;
        this.description = description;
        this.version = version;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                name + "@" + connection.toString() + "->" + serviceName);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean connected() {
        return true;
    }

    @Override
    public boolean active() {
        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public RemoteDeviceMessage sendAndReceive(RemoteDeviceMessage message) {
        try {
            final byte[] received = connection.sendAndReceive(
                    new UDPMessage(message).toByteArray(), 10000);
            return new UDPMessage(received).message;
        } catch (SocketException e) {
            return Timeout;
        } catch (IOException e) {
            return Timeout;
        }
    }

    @Override
    public void send(RemoteDeviceMessage message) {
        try {
            connection.send(new UDPMessage(message).toByteArray());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
