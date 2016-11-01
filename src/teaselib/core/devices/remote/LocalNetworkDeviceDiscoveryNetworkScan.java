/**
 * 
 */
package teaselib.core.devices.remote;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class LocalNetworkDeviceDiscoveryNetworkScan
        extends LocalNetworkDeviceDiscovery {

    @Override
    void searchDevices() throws InterruptedException {
        List<Future<Boolean>> futures = submitSearches();
        addDevices(futures);
    }

    private List<Future<Boolean>> submitSearches() {
        ExecutorService es = Executors.newFixedThreadPool(256);
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        try {
            List<Subnet> subnets = enumerateNetworks();
            for (Subnet subnet : subnets) {
                searchSubnet(es, futures, subnet);
            }
        } catch (SocketException e) {
            LocalNetworkDevice.logger.error(e.getMessage(), e);
        }
        es.shutdown();
        return futures;
    }

    private void searchSubnet(ExecutorService es, List<Future<Boolean>> futures,
            Subnet subnet) {
        LocalNetworkDevice.logger.info("Scanning " + subnet.toString());
        for (InetAddress ip : subnet) {
            futures.add(es.submit(serviceLookup(ip)));
        }
    }

    private Callable<Boolean> serviceLookup(final InetAddress ip) {
        Callable<Boolean> createDevice = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                UDPConnection connection = new UDPConnection(ip,
                        LocalNetworkDevice.Port);
                try {
                    fireDeviceDiscovered(
                            new UDPMessage(
                                    connection.sendAndReceive(
                                            new UDPMessage(RemoteDevice.Id)
                                                    .toByteArray(),
                                            1000)).message);
                    return true;
                } catch (SocketTimeoutException e) {
                    return false;
                } catch (Exception e) {
                    throw e;
                } finally {
                    connection.close();
                }
            }
        };
        return createDevice;
    }

    private static void addDevices(List<Future<Boolean>> futures)
            throws InterruptedException {
        for (Future<Boolean> f : futures) {
            try {
                f.get();
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
