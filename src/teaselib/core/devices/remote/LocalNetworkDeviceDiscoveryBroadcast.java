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

class LocalNetworkDeviceDiscoveryBroadcast extends LocalNetworkDeviceDiscovery {
    Map<InterfaceAddress, Thread> discoveryThreads = new HashMap<InterfaceAddress, Thread>();

    @Override
    void searchDevices(Map<String, LocalNetworkDevice> deviceCache)
            throws InterruptedException {
        try {
            List<InterfaceAddress> networks = networks();
            updateSocketReceiveThreads(networks);
            for (InterfaceAddress intefaceAddess : networks) {
                sendBroadcastIDMessage(intefaceAddess);
            }
        } catch (SocketException e) {
            LocalNetworkDevice.logger.error(e.getMessage(), e);
        }
    }

    private static void sendBroadcastIDMessage(
            InterfaceAddress interfaceAddress) {
        InetAddress broadcastAddress = interfaceAddress.getBroadcast();
        LocalNetworkDevice.logger.info(
                "Sending broadcast message to " + broadcastAddress.toString());
        try {
            UDPConnection connection = new UDPConnection(broadcastAddress, 666);
            connection.send(new UDPMessage(RemoteDevice.Id).toByteArray());
        } catch (IOException e) {
            LocalNetworkDevice.logger.error(e.getMessage(), e);
        }
    }

    class SocketThread extends Thread {

        final UDPConnection connection;

        SocketThread(InterfaceAddress interfaceAddress) throws SocketException {
            connection = new UDPConnection(interfaceAddress.getBroadcast(),
                    LocalNetworkDevice.Port);
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    byte[] received = connection.receive();
                    UDPMessage udpMessage = new UDPMessage(received);
                    RemoteDeviceMessage device = udpMessage.message;
                    if ("services".equals(device.command)) {
                        fireDeviceDiscovered(device);
                    }
                } catch (IOException e) {
                    LocalNetworkDevice.logger.error(e.getMessage(), e);
                }
            }
        }

    }

    private void updateSocketReceiveThreads(List<InterfaceAddress> networks) {
        for (InterfaceAddress interfaceAddress : networks) {
            if (!discoveryThreads.containsKey(interfaceAddress)) {
                try {
                    discoveryThreads.put(interfaceAddress,
                            new SocketThread(interfaceAddress));
                } catch (SocketException e) {
                    LocalNetworkDevice.logger.error(e.getMessage(), e);
                }
            }
        }
    }

    private void fireDeviceDiscovered(RemoteDeviceMessage device) {
        // TODO add listener handling and add new devices to device cache
    }
}