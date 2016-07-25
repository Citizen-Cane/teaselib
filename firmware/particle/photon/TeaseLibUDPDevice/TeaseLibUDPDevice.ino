#include "UDPMessage.h"

// UDP Port used for two way communication
unsigned int localPort = 666;

// An UDP instance to let us send and receive packets over UDP
UDP socket;

void setup() {
  // start the UDP
  socket.begin(localPort);

  // Print your device IP Address via serial
  Serial.begin(9600);
  while(!Serial.available()) Particle.process();
  Serial.println(WiFi.localIP());
}

int packetHeaderSize = 4;
//struct packerHeader
void loop() {
  // Check if data has been received
  if (socket.parsePacket() > 0) {
    Serial.print("Packet received: ");
    // Receive packet in buffer
    char buffer[1024];
    const int packetSize = socket.read(buffer, 1024);
    const int packetNumber = UDPMessage::readShort(buffer);
    Serial.print("#");
    Serial.print(packetNumber, DEC);
    if (packetSize - 4 != UDPMessage::readShort(&buffer[2])) {
      Serial.println("wrong size");
    }
    else if (!UDPMessage::isValid(buffer, packetSize - packetHeaderSize, packetHeaderSize)) {
      Serial.println(" not valid");
    }
    else {
        // skip packet number, packet size
      UDPMessage received(&buffer[packetHeaderSize], packetSize - packetHeaderSize);
      Serial.print(" ");
      Serial.print(received.command);
      Serial.print(" #p=");
      Serial.println(received.parameterCount, DEC);
      for(int i = 0; i < received.parameterCount; i++) {
        Serial.println(received.parameters[i]);
      }
      if (received.binarySize > 0 && received.binary != NULL) {
        Serial.print("plus binary data = ");
        Serial.println(received.binarySize, DEC);
      }
      // Reply
      IPAddress ipAddress = socket.remoteIP();
      int port = socket.remotePort();
      const char* parameters[] = {"1","ident","0.01"};
      UDPMessage status("services", parameters, sizeof(parameters));
      const int messageSize = status.toBuffer(&buffer[packetHeaderSize]);
      if (!UDPMessage::isValid(buffer, packetSize - packetHeaderSize, packetHeaderSize)) {
        Serial.println("response not valid");
        return;
      }
      UDPMessage::writeShort(buffer, packetNumber);
      UDPMessage::writeShort(&buffer[2], messageSize);
      return; // crashes on sendPacket() - toBuffer() wrong - invalid packet
      socket.sendPacket(buffer, packetHeaderSize + messageSize, ipAddress, port);
    }
  }
}
