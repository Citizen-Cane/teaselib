/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import teaselib.TeaseLib;
import teaselib.core.ScriptInterruptedException;
import teaselib.core.devices.DeviceCache;

/**
 * @author Citizen-Cane
 *
 */
public class LocalNetworkDevice implements RemoteDevice {
    private static final String DeviceClassName = "UDPRemoteDevice";

    private static final int Port = 666;

    private static final UDPMessage id = new UDPMessage("id",
            Arrays.asList("test"), new byte[] { 1, 2, 3 });

    public static final DeviceCache.Factory<RemoteDevice> Factory = new DeviceCache.Factory<RemoteDevice>() {
        final Map<String, LocalNetworkDevice> devices = new LinkedHashMap<String, LocalNetworkDevice>();

        @Override
        public String getDeviceClass() {
            return LocalNetworkDevice.DeviceClassName;
        }

        @Override
        public List<String> getDevices() {
            final ExecutorService es = Executors.newFixedThreadPool(256);
            final List<Future<List<LocalNetworkDevice>>> futures = new ArrayList<Future<List<LocalNetworkDevice>>>();
            try {
                List<Subnet> subnets = enumerateNetworks();
                for (Subnet subnet : subnets) {
                    TeaseLib.instance().log
                            .info("Scanning " + subnet.toString());
                    for (final InetAddress ip : subnet) {
                        futures.add(es.submit(
                                new Callable<List<LocalNetworkDevice>>() {
                                    @Override
                                    public List<LocalNetworkDevice> call()
                                            throws Exception {
                                        final UDPConnection udpClient = new UDPConnection(
                                                ip, Port);
                                        return getServices(udpClient,
                                                new UDPMessage(udpClient
                                                        .sendAndReceive(
                                                                id.toByteArray(),
                                                                1000)));
                                    }

                                    private List<LocalNetworkDevice> getServices(
                                            UDPConnection connection,
                                            UDPMessage status)
                                            throws SocketException {
                                        int i = 0;
                                        String name = status.parameters
                                                .get(i++);
                                        int serviceCount = Integer.parseInt(
                                                status.parameters.get(i++));
                                        List<LocalNetworkDevice> devices = new ArrayList<LocalNetworkDevice>(
                                                (status.parameters.size() - i)
                                                        / 2);
                                        for (int j = 0; j < serviceCount; j++) {
                                            String serviceName = status.parameters
                                                    .get(i++);
                                            String version = status.parameters
                                                    .get(i++);
                                            devices.add((new LocalNetworkDevice(
                                                    name, serviceName, version,
                                                    connection)));
                                            if (j < serviceCount) {
                                                connection = new UDPConnection(
                                                        connection.getAddress(),
                                                        connection.getPort());
                                                continue;
                                            } else
                                                break;
                                        }
                                        return devices;
                                    }
                                }));
                    }
                }
            } catch (SocketException e) {
                TeaseLib.instance().log.error(this, e);
            }
            es.shutdown();
            for (final Future<List<LocalNetworkDevice>> f : futures) {
                try {
                    List<LocalNetworkDevice> detectedDevices = f.get();
                    for (LocalNetworkDevice device : detectedDevices) {
                        this.devices.put(device.getDevicePath(), device);
                    }
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof SocketTimeoutException) {
                        // Expected
                    } else {
                        TeaseLib.instance().log.error(this, e);
                    }
                }
            }
            return new ArrayList<String>(this.devices.keySet());
        }

        @Override
        public LocalNetworkDevice getDevice(String devicePath) {
            // TODO create new and remember in map
            return devices.get(devicePath);
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
    private final String version;

    private UDPConnection connection;

    // TODO rescan network, reconnect to same device at different address

    LocalNetworkDevice(String name, String serviceName, String version,
            UDPConnection connection) {
        this.name = name;
        this.serviceName = serviceName;
        this.connection = connection;
        this.version = version;
    }

    @Override
    public String getDevicePath() {
        return DeviceCache.createDevicePath(DeviceClassName,
                connection.toString());
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
    public String getVersion() {
        return version;
    }
}
