//#include "Particle.h"
//#include "Servo.h"

#include "KeyReleaseService.h"

const char* const KeyReleaseService::Name = "KeyRelease";
const char* const KeyReleaseService::Version = "0.01";

const KeyReleaseService::Actuator KeyReleaseService::ShortRelease = {RX, 30 /* minutes*/ };
const KeyReleaseService::Actuator KeyReleaseService::LongRelease = {TX, 60 /* minutes*/ };
const KeyReleaseService::Actuator* KeyReleaseService::DefaultSetup[] = {&ShortRelease, &LongRelease};
const int KeyReleaseService::DefaultSetupSize = 2;

KeyReleaseService::KeyReleaseService(const Actuator**, const int actuators)
: TeaseLibService(Name, Version) {
}

void KeyReleaseService::setup() {
}

int KeyReleaseService::process(const UDPMessage& received, char* buffer) {
  const char* parameters[] = {"2"};
  return UDPMessage("actuators", parameters, 1).toBuffer(buffer);
}
