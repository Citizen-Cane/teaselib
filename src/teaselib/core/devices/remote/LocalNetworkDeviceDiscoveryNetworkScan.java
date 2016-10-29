/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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

    private Callable<List<LocalNetworkDevice>> serviceLookup(
            final InetAddress ip) {
        Callable<List<LocalNetworkDevice>> createDevice = new Callable<List<LocalNetworkDevice>>() {
            @Override
            public List<LocalNetworkDevice> call() throws Exception {
                UDPConnection connection = new UDPConnection(ip,
                        LocalNetworkDevice.Port);
                try {
                    return getServices(new UDPMessage(connection.sendAndReceive(
                            new UDPMessage(RemoteDevice.Id).toByteArray(),
                            1000)).message);
                } catch (SocketTimeoutException e) {
                    return Collections.EMPTY_LIST;
                } catch (Exception e) {
                    throw e;
                } finally {
                    connection.close();
                }
            }

        };
        return createDevice;
    }

    private static void addDevices(Map<String, LocalNetworkDevice> deviceCache,
            final List<Future<List<LocalNetworkDevice>>> futures)
            throws InterruptedException {
        for (Future<List<LocalNetworkDevice>> f : futures) {
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

    /**
     * @param remoteDeviceListener
     */
    public void addRemoteDeviceDiscoveryListener(
            RemoteDeviceListener remoteDeviceListener) {
        remoteDeviceListeners.add(remoteDeviceListener);
    }

}