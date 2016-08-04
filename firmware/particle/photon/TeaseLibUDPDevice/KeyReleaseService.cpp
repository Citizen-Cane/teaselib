#include "KeyReleaseService.h"

const char* const KeyReleaseService::Name = "KeyRelease";
const char* const KeyReleaseService::Description = "Servo-based key release mechanism";
const char* const KeyReleaseService::Version = "0.01";

const KeyReleaseService::Actuator KeyReleaseService::ShortRelease = {RX, 30 /* minutes*/ , 60 /* minutes*/};
const KeyReleaseService::Actuator KeyReleaseService::LongRelease = {TX, 60 /* minutes*/ , 120 /* minutes*/};
const KeyReleaseService::Actuator* KeyReleaseService::DefaultSetup[] = {&ShortRelease, &LongRelease};
const int KeyReleaseService::DefaultSetupSize = 2;

KeyReleaseService::KeyReleaseService(const Actuator** actuators, const int actuatorCount)
: TeaseLibService(Name, Description, Version)
, actuators(actuators)
, servos(new Servo[actuatorCount])
, durations(new Duration[actuatorCount])
, actuatorCount(actuatorCount)
, sessionKey(createSessionKey())
, timer(60 * 1000, &KeyReleaseService::timerCallback, *this)
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
      durations[i].actuator = actuators[i];
      durations[i].clear();
      releaseKey(i);
      delay(1000);
      Serial.print("Actuator ");
      Serial.print(i, DEC);
      Serial.print(": default=");
      Serial.print(durations[i].actuator->defaultMinutes, DEC);
      Serial.print("m, maximum=");
      Serial.print(durations[i].actuator->maximumMinutes, DEC);
      Serial.println("m");
  }
  timer.start();
}

// starting and stopping restarts the timer, and is done in write command only
// to avoid a never-ending test by polling to often (disabled some timer stops)

int KeyReleaseService::process(const UDPMessage& received, char* buffer) {
  if (isCommand(received, "actuators")) {
    const char count[2] = {'0' + min(9, actuatorCount) , 0};
    const char* parameters[] = {count};
    return UDPMessage("count", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "arm" /* actuator */)) {
    timer.stop();
    const int index = atol(received.parameters[0]);
    releaseKey(index);
    durations[index].arm();
    delay(1000);
    armKey(index);
    timer.start();
    return Ok.toBuffer(buffer);
  }
  else if (isCommand(received, "start" /* actuator minutes */)) {
    timer.stop();
    const int index = atol(received.parameters[0]);
    Duration& duration = durations[index];
    const int minutes = atol(received.parameters[1]);
    durations[index].start(minutes);
    timer.start();
    const char* parameters[] = {sessionKey};
    return UDPMessage("releasekey", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "add" /* actuator minutes */)) {
    timer.stop();
    const int index = atol(received.parameters[0]);
    const int minutes = atol(received.parameters[1]);
    durations[index].add(minutes);
    char actualMinutes[4];
    sprintf(actualMinutes, "%d", durations[index].remainingMinutes);
    timer.start();
    const char* parameters[] = {actualMinutes};
    return UDPMessage("count", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "available" /* actuator minutes */)) {
    const int index = atol(received.parameters[0]);
    char minutes[4];
    sprintf(minutes, "%d", durations[index].actuator->maximumMinutes - durations[index].elapsedMinutes);
    const char* parameters[] = {minutes};
    return UDPMessage("count", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "remaining" /* actuator minutes */)) {
    const int index = atol(received.parameters[0]);
    char minutes[4];
    sprintf(minutes, "%d", durations[index].remainingMinutes);
    const char* parameters[] = {minutes};
    return UDPMessage("count", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "running" /* actuator minutes */)) {
    const int index = atol(received.parameters[0]);
    const char* parameters[] = {durations[index].running ? "1" : "0"};
    return UDPMessage("count", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "release" /* actuator releaseKey */)) {
    timer.stop();
    const int index = atol(received.parameters[0]);
    durations[index].clear();
    releaseKey(index);
    timer.start();
    return Ok.toBuffer(buffer);
  }
  else  {
    return 0;
  }
}

void KeyReleaseService::timerCallback() {
  for(int i = 0; i < actuatorCount; i++) {
    Duration& duration = durations[i];
    if (durations[i].running) {
      if(durations[i].advance()) {
        continue;
      } else {
        releaseKey(i);
      }
    }
  }
}

void KeyReleaseService::armKey(const int index) {
  servos[index].write(0);
}

void KeyReleaseService::releaseKey(const int index) {
  servos[index].write(180);
}

const void KeyReleaseService::Duration::arm() {
  running = true;
  elapsedMinutes = 0;
  remainingMinutes = actuator->defaultMinutes;
}

const int KeyReleaseService::Duration::start(const int minutes) {
  remainingMinutes = minutes;
  return min(remainingMinutes, actuator->maximumMinutes - elapsedMinutes);
}

const int KeyReleaseService::Duration::add(const int minutes) {
  remainingMinutes += minutes;
  return min(remainingMinutes, actuator->maximumMinutes - elapsedMinutes);
}

const bool KeyReleaseService::Duration::advance() {
  if (running) {
    if (remainingMinutes > 0 && elapsedMinutes < actuator->maximumMinutes) {
      elapsedMinutes++;
      remainingMinutes--;
    }
    else {
      running = false;
    }
  }
  return running;
}

const void KeyReleaseService::Duration::clear() {
  running = false;
  elapsedMinutes = 0;
  remainingMinutes = 0;
}
