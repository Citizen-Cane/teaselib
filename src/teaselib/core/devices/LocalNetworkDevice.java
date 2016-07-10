/**
 * 
 */
package teaselib.core.devices;

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

/**
 * @author someone
 *
 */
public class LocalNetworkDevice implements RemoteDevice {
    private static final String DeviceClassName = "UDPRemoteDevice";

    private static final int Port = 6666;

    public static final DeviceCache.Factory<RemoteDevice> Factory = new DeviceCache.Factory<RemoteDevice>() {
        final Map<String, LocalNetworkDevice> devices = new LinkedHashMap<String, LocalNetworkDevice>();

        @Override
        public String getDeviceClass() {
            return LocalNetworkDevice.DeviceClassName;
        }

        @Override
        public List<String> getDevices() {
            final ExecutorService es = Executors.newFixedThreadPool(256);
            final List<Future<UDPClient>> futures = new ArrayList<Future<UDPClient>>();
            try {
                List<Subnet> subnets = enumerateNetworks();
                for (Subnet subnet : subnets) {
                    TeaseLib.instance().log
                            .info("Scanning " + subnet.toString());
                    for (InetAddress ip : subnet) {
                        final UDPClient udpClient = new UDPClient(ip, Port);
                        futures.add(es.submit(new Callable<UDPClient>() {
                            @Override
                            public UDPClient call() throws Exception {
                                udpClient.sendAndReceive("id", 1000);
                                return udpClient;
                            }
                        }));
                    }
                }
            } catch (SocketException e) {
                TeaseLib.instance().log.error(this, e);
            }
            es.shutdown();
            for (final Future<UDPClient> f : futures) {
                try {
                    UDPClient udpClient = f.get();
                    if (udpClient != null) {
                        // TODO packet sent twice, better receive ident in scan
                        final String[] id = udpClient.sendAndReceive("id", 1000)
                                .split("\n");
                        String name = id[0];
                        String serviceName = id[1];
                        String version = id[2];
                        // TODO query service, add device for each
                        final LocalNetworkDevice localNetworkDevice = new LocalNetworkDevice(
                                name, serviceName, version, udpClient);
                        this.devices.put(localNetworkDevice.getDevicePath(),
                                localNetworkDevice);
                    }
                } catch (InterruptedException e) {
                    throw new ScriptInterruptedException();
                } catch (SocketException e) {
                    TeaseLib.instance().log.error(this, e);
                } catch (IOException e) {
                    TeaseLib.instance().log.error(this, e);
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

    private UDPClient connection;

    // TODO rescan network, reconnect to same device at different address

    LocalNetworkDevice(String name, String serviceName, String version,
            UDPClient connection) {
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
    public boolean active() {
        return true;
    }

    @Override
    public void release() {
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
