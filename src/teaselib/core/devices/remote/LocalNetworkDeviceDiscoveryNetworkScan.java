/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class LocalNetworkDeviceDiscoveryNetworkScan
        extends LocalNetworkDeviceDiscovery {

    @Override
    void searchDevices(Map<String, LocalNetworkDevice> deviceCache)
            throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(256);
        List<Future<List<LocalNetworkDevice>>> futures = new ArrayList<Future<List<LocalNetworkDevice>>>();
        try {
            List<Subnet> subnets = enumerateNetworks();
            for (Subnet subnet : subnets) {
                LocalNetworkDevice.logger.info("Scanning " + subnet.toString());
                for (InetAddress ip : subnet) {
                    futures.add(es.submit(serviceLookup(ip)));
                }
            }
        } catch (SocketException e) {
            LocalNetworkDevice.logger.error(e.getMessage(), e);
        }
        es.shutdown();
        addDevices(deviceCache, futures);
    }

    private static Callable<List<LocalNetworkDevice>> serviceLookup(
            final InetAddress ip) {
        Callable<List<LocalNetworkDevice>> createDevice = new Callable<List<LocalNetworkDevice>>() {
            @Override
            public List<LocalNetworkDevice> call() throws Exception {
                UDPConnection connection = new UDPConnection(ip,
                        LocalNetworkDevice.Port);
                try {
                    return getServices(connection,
                            new UDPMessage(connection.sendAndReceive(
                                    new UDPMessage(RemoteDevice.Id)
                                            .toByteArray(),
                                    1000)));
                } catch (SocketTimeoutException e) {
                    return Collections.EMPTY_LIST;
                } catch (Exception e) {
                    throw e;
                } finally {
                    connection.close();
                }
            }

            private List<LocalNetworkDevice> getServices(
                    UDPConnection connection, UDPMessage status)
                    throws SocketException, UnknownHostException {
                int i = 0;
                String name = status.message.parameters.get(i++);
                InetAddress address = InetAddress
                        .getByName(status.message.parameters.get(i++));
                int serviceCount = Integer
                        .parseInt(status.message.parameters.get(i++));
                List<LocalNetworkDevice> devices = new ArrayList<LocalNetworkDevice>(
                        (status.message.parameters.size() - i) / 2);
                for (int j = 0; j < serviceCount; j++) {
                    String serviceName = status.message.parameters.get(i++);
                    String description = status.message.parameters.get(i++);
                    String version = status.message.parameters.get(i++);
                    devices.add((new LocalNetworkDevice(name,
                            new UDPConnection(address, connection.getPort()),
                            serviceName, description, version)));
                }
                return devices;
            }
        };
        return createDevice;
    }

    private static void addDevices(Map<String, LocalNetworkDevice> deviceCache,
            final List<Future<List<LocalNetworkDevice>>> futures)
            throws InterruptedException {
        for (final Future<List<LocalNetworkDevice>> f : futures) {
            try {
                List<LocalNetworkDevice> detectedDevices = f.get();
                for (LocalNetworkDevice device : detectedDevices) {
                    deviceCache.put(device.getDevicePath(), device);
                }
            } catch (ExecutionException e) {
                LocalNetworkDevice.logger.error(e.getMessage(), e);
            }
        }
    }

    List<Subnet> enumerateNetworks() throws SocketException {
        List<Subnet> subnets = new Vector<Subnet>();
        for (InterfaceAddress interfaceAddress : networks()) {
            subnets.add(new Subnet(interfaceAddress));
        }
        return subnets;
    }

}