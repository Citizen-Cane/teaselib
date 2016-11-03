#include "UDPMessage.h"
#include "KeyReleaseService.h"

KeyReleaseService keyRelease(KeyReleaseService::DefaultSetup, KeyReleaseService::DefaultSetupSize);
//TimelockService timeLock;
// BatteryChargeService batteryCharge;

TeaseLibService* services[] = {&keyRelease};
const int serviceCount = sizeof(services)/sizeof(TeaseLibService*);

UDP socket;
const unsigned int localPort = 666;
String deviceAddress;

const int PacketHeaderSize = 4;
const int BufferSize = 1024;
char buffer[BufferSize];

void process(const UDPMessage& received, const int packetNumber);
void send(const UDPMessage& message, const int packetNumber);
void send(char* buffer, const int messageSize, const int packetNumber);
void send(const IPAddress& ipAddress, const int port, char* buffer, const int messageSize, const int packetNumber);
void println(const UDPMessage& packet);

void setup() {
  // start UDP
  socket.begin(localPort);

  Serial.begin(9600);
  // Wait for key press on serial
  // while(!Serial.available()) Particle.process();
  IPAddress localIP =WiFi.localIP();
  Serial.println(localIP);
  // setup services
  deviceAddress = String(localIP);
  TeaseLibService::setup(services, serviceCount, deviceAddress);
  const int responseSize = TeaseLibService::processIdPacket(&buffer[PacketHeaderSize]);
  if (responseSize > 0) {
    IPAddress subnetMask = WiFi.subnetMask();
    Serial.println(subnetMask);
    IPAddress broadcast = IPAddress(255,255,255,255);
    for(uint8_t i = 0; i < 4; i++) {
      const uint8_t bits = (~subnetMask[i]) | localIP[i];
      broadcast[i] = bits;
    }
    Serial.println(broadcast);
    send(broadcast, localPort, buffer, responseSize, 0);
  }
}

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
    const int responseSize = TeaseLibService::processIdPacket(&buffer[PacketHeaderSize]);
    if (responseSize > 0) {
      send(buffer, responseSize, packetNumber);
    }
  }
  else if (strcmp(TeaseLibService::Sleep, received.command) == 0) {
    TeaseLibService::SleepMode sleepMode = TeaseLibService::DeepSleep;
    const unsigned int sleepMinutes = TeaseLibService::processSleepPacket(received, sleepMode);
    char minutes[4];
    sprintf(minutes, "%d", sleepMinutes);
    const char* parameters[] = {minutes};
    const int responseSize = UDPMessage("count", parameters, 1).toBuffer(&buffer[PacketHeaderSize]);
    send(buffer, responseSize, packetNumber);
    socket.flush();
    if (sleepMinutes > 0) {
      if (sleepMode == TeaseLibService::DeepSleep) {
        System.sleep(SLEEP_MODE_DEEP, 60 * sleepMinutes);
      } else if (sleepMode == TeaseLibService::LightSleep) {
        System.sleep(60 * sleepMinutes);
      }
    }
  }
  else {
    const unsigned int responseSize = TeaseLibService::processPacket(received, &buffer[PacketHeaderSize]);
    if (responseSize > 0) {
      send(buffer, responseSize, packetNumber);
    }
  }
}

void send(const UDPMessage& message, const int packetNumber) {
  const int messageSize = message.toBuffer(&buffer[PacketHeaderSize]);
  send(buffer, messageSize, packetNumber);
}

void send(char* buffer, const int messageSize, const int packetNumber) {
  const IPAddress ipAddress = socket.remoteIP();
  const int port = socket.remotePort();
  send(ipAddress, port, buffer, messageSize, packetNumber);
}

void send(const IPAddress& ipAddress, const int port, char* buffer, const int messageSize, const int packetNumber) {
  UDPMessage::writeShort(buffer, packetNumber);
  UDPMessage::writeShort(&buffer[2], messageSize);
  if (!UDPMessage::isValid(buffer, messageSize, PacketHeaderSize)) {
    Serial.print("Response packet not valid:");
    println(UDPMessage(&buffer[PacketHeaderSize], messageSize));
    return;
  }
  Serial.print("Response =");
  println(UDPMessage(&buffer[PacketHeaderSize], messageSize));
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
