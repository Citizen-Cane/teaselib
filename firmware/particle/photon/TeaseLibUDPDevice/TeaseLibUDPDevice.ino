//#include "Particle.h"
//#include "Servo.h"

#include "UDPMessage.h"
#include "KeyReleaseService.h"

KeyReleaseService keyRelease(KeyReleaseService::DefaultSetup, KeyReleaseService::DefaultSetupSize);
//TimelockService timeLock;
// BatteryChargeService batteryCharge;

TeaseLibService* services[] = {&keyRelease};
const int serviceCount = sizeof(services)/sizeof(TeaseLibService*);

UDP socket;
unsigned int localPort = 666;

void setup() {
  // start UDP
  socket.begin(localPort);

  Serial.begin(9600);
  // Wait for key press on serial
  //while(!Serial.available()) Particle.process();
  Serial.println(WiFi.localIP());

  // setup services
  TeaseLibService::setup(services, serviceCount);
}

void process(const UDPMessage& received, const int packetNumber);
void send(const UDPMessage& message, const int packetNumber);
void send(char* buffer, const int messageSize, const int packetNumber);
void println(const UDPMessage& packet);

const int PacketHeaderSize = 4;
const int BufferSize = 1024;
char buffer[BufferSize];

void loop() {
  // Check if data has been received
  if (socket.parsePacket() > 0) {
    Serial.print("Packet received: ");
    // Receive packet in buffer
    const int packetSize = socket.read(buffer, BufferSize);
    const int packetNumber = UDPMessage::readShort(buffer);
    Serial.print("#");
    Serial.print(packetNumber, DEC);
    if (packetSize - 4 != UDPMessage::readShort(&buffer[2])) {
      Serial.println("wrong size");
    }
    else if (!UDPMessage::isValid(buffer, packetSize - PacketHeaderSize, PacketHeaderSize)) {
      Serial.println(" not valid");
    }
    else {
        // skip packet number, packet size
      const UDPMessage received(&buffer[PacketHeaderSize], packetSize - PacketHeaderSize);
      process(received, packetNumber);
    }
  }
}

void process(const UDPMessage& received, const int packetNumber) {
  println(received);
  if (strcmp(TeaseLibService::Id, received.command) == 0) {
    const int responseSize = TeaseLibService::processIdPacket(received, &buffer[PacketHeaderSize]);
    if (responseSize > 0) {
      send(buffer, responseSize, packetNumber);
    }
  }
  else {
    for(int i = 0; i < serviceCount; i++) {
      if (services[i]->canHandle(received.command)) {
        const int responseSize = services[i]->process(received, &buffer[PacketHeaderSize]);
        if (responseSize > 0) {
          send(buffer, responseSize, packetNumber);
        }
        break;
      }
    }
  }
}

void send(const UDPMessage& message, const int packetNumber) {
  const int messageSize = message.toBuffer(&buffer[PacketHeaderSize]);
  send(buffer, messageSize, packetNumber);
}

void send(char* buffer, const int messageSize, const int packetNumber) {
  UDPMessage::writeShort(buffer, packetNumber);
  UDPMessage::writeShort(&buffer[2], messageSize);
  if (!UDPMessage::isValid(buffer, messageSize, PacketHeaderSize)) {
    Serial.print("Response packet not valid:");
    println(UDPMessage(&buffer[PacketHeaderSize], messageSize));
    return;
  }
  Serial.print("Response =");
  println(UDPMessage(&buffer[PacketHeaderSize], messageSize));
  IPAddress ipAddress = socket.remoteIP();
  int port = socket.remotePort();
  socket.sendPacket(buffer, PacketHeaderSize + messageSize, ipAddress, port);
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
