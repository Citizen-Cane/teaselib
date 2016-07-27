#include "UDPMessage.h"

unsigned int localPort = 666;

UDP socket;

void setup() {
  // start UDP
  socket.begin(localPort);

  Serial.begin(9600);
  // Wait for key press on serial
//  while(!Serial.available()) Particle.process();
  Serial.println(WiFi.localIP());
}

void println(const UDPMessage& packet);

int packetHeaderSize = 4;

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
      println(received);
      // Reply
      IPAddress ipAddress = socket.remoteIP();
      int port = socket.remotePort();
      String deviceId = "My Photon" + System.deviceID();
      const char* parameters[] = {deviceId, "2","KeyRelease","0.01", "Timelock","0.01"};
      UDPMessage status("services", parameters, sizeof(parameters)/sizeof(char*));
      const int messageSize = status.toBuffer(&buffer[packetHeaderSize]);
      UDPMessage::writeShort(buffer, packetNumber);
      UDPMessage::writeShort(&buffer[2], messageSize);
      if (!UDPMessage::isValid(buffer, messageSize, packetHeaderSize)) {
        Serial.print("Response packet not valid:");
        println(UDPMessage(&buffer[packetHeaderSize], messageSize));
        return;
      }
      Serial.print("Response =");
      println(UDPMessage(&buffer[packetHeaderSize], messageSize));
      socket.sendPacket(buffer, packetHeaderSize + messageSize, ipAddress, port);
    }
  }
}

void println(const UDPMessage& packet) {
  Serial.print(" ");
  Serial.print(packet.command);
  Serial.print(" #p=");
  Serial.println(packet.parameterCount, DEC);
  for(int i = 0; i < packet.parameterCount; i++) {
    Serial.println(packet.parameters[i]);
  }
  if (packet.binarySize > 0 && packet.binary != NULL) {
    Serial.print("plus binary data = ");
    Serial.println(packet.binarySize, DEC);
  }
}
