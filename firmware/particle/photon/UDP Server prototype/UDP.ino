// EXAMPLE USAGE

// UDP Port used for two way communication
unsigned int localPort = 6666;

// An UDP instance to let us send and receive packets over UDP
UDP Udp;

void setup() {
  // start the UDP
  Udp.begin(localPort);

  // Print your device IP Address via serial
  Serial.begin(9600);
  Serial.println(WiFi.localIP());
}

char* id = "TeaseLib Self-Bondage Key Release\nkeyrelease\n0.01proto";

void loop() {
  // Check if data has been received
  if (Udp.parsePacket() > 0) {

    // Read first char of data received
    char c = Udp.read();

    // Ignore other chars
    Udp.flush();

    // Store sender ip and port
    IPAddress ipAddress = Udp.remoteIP();
    int port = Udp.remotePort();

    // Echo back data to sender
//    Udp.beginPacket(ipAddress, port);
//    Udp.write(c);
    Udp.sendPacket(id, strlen(id)+1,ipAddress, port);
//    Udp.endPacket();
  }
}
