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
    Serial.println("Packet received");
    // Receive packet in buffer
    char buffer[1024];
    socket.read(buffer, 1024);
      // skip packet number, packet size
    Serial.println(&buffer[packetHeaderSize + 3]);
    UDPMessage received(&buffer[packetHeaderSize]);
    for(int i = 0; i < received.parameterCount; i++) {
      Serial.println(received.parameters[i]);
    }
    if (received.binarySize > 0 && received.binary != NULL) {
      Serial.println("plus binary data");
    }
    // receiving works
    return;
    // Reply
    IPAddress ipAddress = socket.remoteIP();
    int port = socket.remotePort();
    const char* parameters[] = {"1","ident","0.01"};
    UDPMessage status("services", parameters, sizeof(parameters));
    socket.sendPacket(status.buffer, status.bufferSize, ipAddress, port);
  }
}
