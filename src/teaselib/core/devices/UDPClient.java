/**
 * 
 */
package teaselib.core.devices;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * @author someone
 *
 */
public class UDPClient {
    final InetAddress address;
    final int port;
    final DatagramSocket clientSocket;

    public UDPClient(InetAddress address, int port) throws SocketException {
        this.address = address;
        this.port = port;
        this.clientSocket = new DatagramSocket();
    }

    public void send(String data) throws IOException {
        byte[] sendData = new byte[1024];
        sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData,
                sendData.length, address, port);
        clientSocket.send(sendPacket);
    }

    public String sendAndReceive(String data, int timeoutMillis)
            throws IOException, SocketException {
        send(data);
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData,
                receiveData.length);
        clientSocket.setSoTimeout(timeoutMillis);
        clientSocket.receive(receivePacket);
        String answer = new String(receivePacket.getData());
        // clientSocket.close();
        return answer;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return address.toString() + ":" + port;
    }

}
