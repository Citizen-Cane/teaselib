#include "KeyReleaseService.h"

const char* const KeyReleaseService::Name = "KeyRelease";
const char* const KeyReleaseService::Description = "Servo-based key release mechanism";
const char* const KeyReleaseService::Version = "0.01";

/* The default setup:
 - two servos, one for short term, one for long term self-bondage or restraint
 - servos open at an angle of 120°
   - 180° should be possible according to the Photon docs, but servos seem to be to cheap to work that way
   - anyway 90° to 120° rotation is sufficient for holding and releasing the key
 - on startup the servos will turn down immediately releae any keys
 - they stay down until a hook is "armed", then the servo goes up and a key can be attached
*/

/*
 How to make the key hooks:
 - cut off all but two orthognally oriented arms from the servo horn
 - place the horn on the axis so that you can hang the key at the hook
 - on releae the servo moves down to let the key slip off the hook
*/

/* Servo orientation:
 - if the servos move in the wrong direction, either
   turn your device upside down or change the default settings to your needs
*/

/* LED state:
  - The LED breathes cyan as usual (connected to the cloud) while the device is inactve.
  - It pulses dark green when any of the servos is ready to receive a key
  - It pulses dark red while any of the timers is counting down
  - It starts breathing again once all keys have been released
  the activity pulse frequence indicates the remaining time until all keys will have been released
*/

const int KeyReleaseService::DefaultSetupSize = 2;
const KeyReleaseService::Actuator KeyReleaseService::ShortRelease = {TX, 30 /* minutes*/ , 60 /* minutes*/, 30, 150};
const KeyReleaseService::Actuator KeyReleaseService::LongRelease = {RX, 60 /* minutes*/ , 120 /* minutes*/, 150, 30};
const KeyReleaseService::Actuator* KeyReleaseService::DefaultSetup[] = {&ShortRelease, &LongRelease};

KeyReleaseService::KeyReleaseService(const Actuator** actuators, const int actuatorCount)
: TeaseLibService(Name, Description, Version)
, actuators(actuators)
, servos(new Servo[actuatorCount])
, durations(new Duration[actuatorCount])
, actuatorCount(actuatorCount)
, sessionKey(createSessionKey())
, releaseTimer(60 * 1000, &KeyReleaseService::releaseTimerCallback, *this)
, ledTimer(1000, &KeyReleaseService::ledTimerCallback, *this)
, status(Idle)
, lastStatus(Idle)
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
      if (i > 0) {
        delay(1000);
      }
      releaseKey(i);
      delay(1000);
      armKey(i);
      delay(1000);
      releaseKey(i);
      Serial.print("Actuator ");
      Serial.print(i, DEC);
      Serial.print(": default=");
      Serial.print(durations[i].actuator->defaultMinutes, DEC);
      Serial.print("m, maximum=");
      Serial.print(durations[i].actuator->maximumMinutes, DEC);
      Serial.println("m");
  }
  releaseTimer.start();
}

// starting and stopping restarts the timer, and is done only in write commands
// to avoid blocking the key release by polling to often

int KeyReleaseService::process(const UDPMessage& received, char* buffer) {
  if (isCommand(received, "actuators")) {
    const char count[2] = {'0' + min(9, actuatorCount) , 0};
    const char* parameters[] = {count};
    return UDPMessage("count", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "arm" /* actuator */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    releaseKey(index);
    durations[index].arm();
    delay(1000);
    armKey(index);
    status = Armed;
    updatePulse();
    ledTimer.start();
    releaseTimer.start();
    return Ok.toBuffer(buffer);
  }
  else if (isCommand(received, "start" /* actuator minutes */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    const int minutes = atol(received.parameters[1]);
    durations[index].start(minutes);
    status = Active;
    updatePulse();
    ledTimer.start();
    releaseTimer.start();
    const char* parameters[] = {sessionKey};
    return UDPMessage("releasekey", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "add" /* actuator minutes */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    const int minutes = atol(received.parameters[1]);
    durations[index].add(minutes);
    updatePulse();
    char actualMinutes[4];
    sprintf(actualMinutes, "%d", durations[index].remainingMinutes);
    releaseTimer.start();
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
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    durations[index].clear();
    releaseKey(index);
    updatePulse();
    releaseTimer.start();
    return Ok.toBuffer(buffer);
  }
  else  {
    return 0;
  }
}

void KeyReleaseService::releaseTimerCallback() {
  if (status == Released) {
    const unsigned int nextReleaseDuration = nextRelease();
    if (nextReleaseDuration > 0) {
      status = Active;
    } else {
      status = Idle;
    }
  }
  // Update durations
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
  updatePulse();
}

unsigned int KeyReleaseService::nextRelease() {
  unsigned int nextReleaseDuration = durations[0].remainingMinutes;
  for(int i = 1; i < actuatorCount; i++) {
    nextReleaseDuration = min(nextReleaseDuration, durations[i].remainingMinutes);
  }
  return nextReleaseDuration;
}

void KeyReleaseService::updatePulse() {
  if (status == Armed) {
    ledTimer.changePeriod(1000);
    ledTimerCallback();
  } else if (status == Active) {
    const unsigned int nextReleaseDuration = nextRelease();
    if (nextReleaseDuration > 0) {
      ledTimer.changePeriod(500 + 200 * nextReleaseDuration);
    }
    else {
      // TODO pulse is never blue, turns white / rose immediately
      status = Released;
      ledTimer.changePeriod(1000);
    }
  }
}

void KeyReleaseService::ledTimerCallback() {
  if (lastStatus != status) {
    // on/off
    if (lastStatus == Idle) {
      RGB.control(true);
    }
    else if (status == Idle) {
      RGB.control(false);
      ledTimer.stop();
    }
    // pulse color
    if (status == Armed) {
      RGB.color(0, 255, 0);
    }
    else if (status == Active) {
      RGB.color(0, 0, 255);
    }
    else if (status == Released) {
      RGB.color(255, 64, 64);
    }
    // Update
    lastStatus = status;
  }
  // Pulse
  RGB.brightness(48);
  delay(10);
  RGB.brightness(0);
}

void KeyReleaseService::armKey(const int index) {
  servos[index].write(actuators[index]->armedAngle);
}

void KeyReleaseService::releaseKey(const int index) {
  servos[index].write(actuators[index]->releasedAngle);
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
