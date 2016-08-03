#include "KeyReleaseService.h"

const char* const KeyReleaseService::Name = "KeyRelease";
const char* const KeyReleaseService::Description = "Servo-based key release mechanism";
const char* const KeyReleaseService::Version = "0.01";

const KeyReleaseService::Actuator KeyReleaseService::ShortRelease = {RX, 30 /* minutes*/ };
const KeyReleaseService::Actuator KeyReleaseService::LongRelease = {TX, 60 /* minutes*/ };
const KeyReleaseService::Actuator* KeyReleaseService::DefaultSetup[] = {&ShortRelease, &LongRelease};
const int KeyReleaseService::DefaultSetupSize = 2;

KeyReleaseService::KeyReleaseService(const Actuator** actuators, const int actuatorCount)
: TeaseLibService(Name, Description, Version)
, actuators(actuators)
, servos(new Servo[actuatorCount])
, durations(new Duration[actuatorCount])
, actuatorCount(actuatorCount)
, sessionKey(createSessionKey())
{
}

const char* const KeyReleaseService::createSessionKey() {
  static int SessionKeySize= 16;
  char* sessionKey = new char[SessionKeySize];
  for(int i = 0; i < SessionKeySize; i++) {
    sessionKey[i] = '0' + rand() % 10;
  }
  return sessionKey;
}

void KeyReleaseService::setup() {
  for(int i = 0; i < actuatorCount; i++) {
      servos[i].attach(actuators[i]->pin);
      durations[i].elapsed = 0;
      durations[i].left = actuators[i]->defaultDuration;
  }
}

int KeyReleaseService::process(const UDPMessage& received, char* buffer) {
  if (isCommand(received, "actuators")) {
    const char count[2] = {'0' + min(9, actuatorCount) , 0};
    const char* parameters[] = {count};
    return UDPMessage("count", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "arm" /* actuator */)) {
    return Ok.toBuffer(buffer);
  }
  else if (isCommand(received, "start" /* actuator minutes */)) {
    const char* parameters[] = {sessionKey};
    return UDPMessage("releasekey", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "addTime" /* actuator minutes */)) {
    return Ok.toBuffer(buffer);
  }
  else if (isCommand(received, "release" /* actuator releaseKey */)) {
    return Ok.toBuffer(buffer);
  }
  else  {
    return 0;
  }
}
