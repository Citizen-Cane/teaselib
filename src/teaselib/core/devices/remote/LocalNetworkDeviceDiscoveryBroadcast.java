/**
 * 
 */
package teaselib.core.devices.remote;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class LocalNetworkDeviceDiscoveryBroadcast extends LocalNetworkDeviceDiscovery {
    Map<InterfaceAddress, Thread> discoveryThreads = new HashMap<InterfaceAddress, Thread>();

    @Override
    void searchDevices(Map<String, LocalNetworkDevice> deviceCache)
            throws InterruptedException {
        try {
            List<InterfaceAddress> networks = networks();
            updateSocketReceiveThreads(networks);
            broadcastToAllInterfaces(networks);
        } catch (SocketException e) {
            LocalNetworkDevice.logger.error(e.getMessage(), e);
        }
        waitForDeviceAddedEvents();
    }

    private void updateSocketReceiveThreads(List<InterfaceAddress> networks) {
        addSOcketThreadsForNewNetworks(networks);
        removeSocketThreadsForVanishedNetworks(networks);
    }

    private void addSOcketThreadsForNewNetworks(
            List<InterfaceAddress> networks) {
        for (InterfaceAddress interfaceAddress : networks) {
            if (!discoveryThreads.containsKey(interfaceAddress)) {
                try {
                    SocketThread socketThread = new SocketThread(
                            interfaceAddress);
                    discoveryThreads.put(interfaceAddress, socketThread);
                    socketThread.start();
                } catch (SocketException e) {
                    LocalNetworkDevice.logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private void removeSocketThreadsForVanishedNetworks(
            List<InterfaceAddress> networks) {
        for (Entry<InterfaceAddress, Thread> entry : discoveryThreads
                .entrySet()) {
            if (networks.contains(entry.getKey())) {
                discoveryThreads.remove(entry.getKey()).interrupt();
            }
        }
    }

    private static void broadcastToAllInterfaces(
            List<InterfaceAddress> networks) {
        for (InterfaceAddress intefaceAddess : networks) {
            try {
                sendBroadcastIDMessage(intefaceAddess);
            } catch (IOException e) {
                LocalNetworkDevice.logger.error(e.getMessage(), e);
            }
        }
    }

    private static void sendBroadcastIDMessage(
            InterfaceAddress interfaceAddress) throws IOException {
        InetAddress broadcastAddress = interfaceAddress.getBroadcast();
        LocalNetworkDevice.logger.info(
                "Sending broadcast message to " + broadcastAddress.toString());
        UDPConnection connection = new UDPConnection(broadcastAddress, 666);
        connection.send(new UDPMessage(RemoteDevice.Id).toByteArray());
    }

    private static void waitForDeviceAddedEvents() throws InterruptedException {
        Thread.sleep(2000);
    }

    class SocketThread extends Thread {
        final UDPConnection connection;

        SocketThread(InterfaceAddress interfaceAddress) throws SocketException {
            connection = new UDPConnection(interfaceAddress.getBroadcast(),
                    LocalNetworkDevice.Port);
            setDaemon(true);
            setName("Local network device discovery receiver on "
                    + interfaceAddress.getBroadcast().toString());
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    byte[] received = connection.receive();
                    RemoteDeviceMessage services = new UDPMessage(
                            received).message;
                    if ("services".equals(services.command)) {
                        fireDeviceDiscovered(services);
                    }
                } catch (IOException e) {
                    LocalNetworkDevice.logger.error(e.getMessage(), e);
                }
            }
        }
    }
}
