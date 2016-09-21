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

/* Servo control:
   When the battery drains and the voltage drops, moving the servos
   drop the voltage down below the amount needed to feed them with a proper PWM signal

   Experimental (non-working, can be activated in source code):
   - To save battery charge servos are attached only for the time it takes to move the shaft to the requested angle.
   - This also solves constant servo readjusting at low battery charge:
     - because the voltage drops when moving the servo, the PWM signal is not properly recognized by the servo anymore,
       resulting in constant readjusting, which drains the battery even more
*/

/* LED state:
  - The LED breathes cyan as usual (connected to the cloud) while the device is inactve.
  - It pulses dark green when any of the servos is ready to receive a key
  - It pulses dark red while any of the timers is counting down
  - It starts breathing again once all keys have been released
  the activity pulse frequence indicates the remaining time until all keys will have been released
*/

/* Sleep modes:
  - Deep sleep is supported if a single release is pending and
  - the requested sleep time is greater than or equal to the relase duration
  Otherwise the device enters light sleep until the next release.
  TODO light sleep to all but last release, then deep sleep to last.
*/

const int KeyReleaseService::DefaultSetupSize = 2;
const KeyReleaseService::Actuator KeyReleaseService::ShortRelease = {TX, 30 /* minutes*/ , 60 /* minutes*/, 30, 150};
const KeyReleaseService::Actuator KeyReleaseService::LongRelease = {RX, 60 /* minutes*/ , 120 /* minutes*/, 150, 30};
const KeyReleaseService::Actuator* KeyReleaseService::DefaultSetup[] = {&ShortRelease, &LongRelease};

KeyReleaseService::KeyReleaseService(const Actuator** actuators, const int actuatorCount)
: TeaseLibService(Name, Description, Version)
, actuators(actuators)
, servoControl(new ServoControl[actuatorCount])
, durations(new Duration[actuatorCount])
, actuatorCount(actuatorCount)
, sessionKey(createSessionKey())
, releaseTimer(60 * 1000, &KeyReleaseService::releaseTimerCallback, *this)
, ledTimer(1000, &KeyReleaseService::ledTimerCallback, *this)
, status(Idle)
{
}

const char* const KeyReleaseService::createSessionKey() {
  static int SessionKeySize= 17;
  char* sessionKey = new char[SessionKeySize];
  int i = 0;
  // TODO Without a proper seed, the session key is always the same
  for(; i < SessionKeySize; i++) {
    sessionKey[i] = '0' + rand() % 10;
  }
  sessionKey[i] = 0;
  return sessionKey;
}

void KeyReleaseService::setup() {
  for(int i = 0; i < actuatorCount; i++) {
      servoControl[i].attach(actuators[i]);
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

unsigned int KeyReleaseService::process(const UDPMessage& received, char* buffer) {
  if (isCommand(received, "actuators")) {
    const char count[2] = {'0' + min(9, actuatorCount) , 0};
    const char* parameters[] = {count};
    return UDPMessage("count", parameters, 1).toBuffer(buffer);
    return createCountMessage(actuatorCount, buffer);
  }
  else if (isCommand(received, "arm" /* actuator */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    releaseKey(index);
    Duration& duration = durations[index];
    duration.arm();
    delay(1000);
    armKey(index);
    updatePulse(Armed);
    releaseTimer.start();
    return Ok.toBuffer(buffer);
  }
  else if (isCommand(received, "start" /* actuator minutes */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    const int minutes = atol(received.parameters[1]);
    durations[index].start(minutes);
    updatePulse(Active);
    releaseTimer.start();
    const char* parameters[] = {sessionKey};
    return UDPMessage("releasekey", parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "add" /* actuator minutes */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    const int minutes = atol(received.parameters[1]);
    Duration& duration = durations[index];
    duration.add(minutes);
    updatePulse(status);
    releaseTimer.start();
    return createCountMessage(duration.remainingMinutes, buffer);
  }
  else if (isCommand(received, "available" /* actuator */)) {
    const int index = atol(received.parameters[0]);
    Duration& duration = durations[index];
    return createCountMessage(duration.actuator->maximumMinutes - duration.elapsedMinutes, buffer);
  }
  else if (isCommand(received, "remaining" /* actuator */)) {
    const int index = atol(received.parameters[0]);
    Duration& duration = durations[index];
    return createCountMessage(duration.remainingMinutes, buffer);
  }
  else if (isCommand(received, "running" /* actuator */)) {
    const int index = atol(received.parameters[0]);
    Duration& duration = durations[index];
    return createCountMessage(duration.running ? 1 : 0, buffer);
  }
  else if (isCommand(received, "release" /* actuator releaseKey */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    // TODO comparre release key string in order to  prevent cheating
    durations[index].clear();
    releaseKey(index);
    updatePulse(Released);
    releaseTimer.start();
    return Ok.toBuffer(buffer);
  }
  else  {
    return 0;
  }
}

unsigned int KeyReleaseService::createCountMessage(unsigned int count, char* buffer) {
  char digits[8];
  sprintf(digits, "%d", count);
  const char* parameters[] = {digits};
  return UDPMessage("count", parameters, 1).toBuffer(buffer);
}


unsigned int KeyReleaseService::sleepRequested(const unsigned int requestedSleepDuration, SleepMode& sleepMode) {
  const unsigned int n = runningReleases();
  if (n == 0) {
    return requestedSleepDuration;
  } else if (n == 1) {
    const unsigned int nextReleaseDuration = nextRelease();
    if (sleepMode == DeepSleep && nextReleaseDuration <= requestedSleepDuration) {
      return nextReleaseDuration;
    } else {
      sleepMode = LightSleep;
      return requestedSleepDuration;
    }
  } else {
      const unsigned int nextReleaseDuration = nextRelease();
      sleepMode = LightSleep;
      return nextReleaseDuration;
  }
}

void KeyReleaseService::releaseTimerCallback() {
  // Resume to Active or Idle after one minute
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
    if (duration.running) {
      if(duration.advance()) {
        continue;
      } else {
        releaseKey(i);
        updatePulse(Released);
      }
    }
  }
  // Update pulse
  if (status != Released) {
    updatePulse(status);
  }
}

unsigned int KeyReleaseService::nextRelease() {
  unsigned int nextReleaseDuration = 0;
  for(int i = 0; i < actuatorCount; i++) {
    nextReleaseDuration = max(nextReleaseDuration, durations[i].remainingMinutes);
  }
  return nextReleaseDuration;
}

unsigned int KeyReleaseService::runningReleases() {
  unsigned int runningReleases = 0;
  for(int i = 0; i < actuatorCount; i++) {
    if (durations[i].running) {
      runningReleases++;
    }
  }
  return runningReleases;
}

void KeyReleaseService::updatePulse(const Status status) {
  this->status = status;
  if (status == Armed) {
    ledTimer.changePeriod(3000);
    RGB.control(true);
    RGB.color(0, 255, 0);
    ledTimer.start();
  } else if (status == Active) {
    const unsigned int nextReleaseDuration = nextRelease();
    if (nextReleaseDuration > 0) {
      ledTimer.changePeriod(500 + 200 * nextReleaseDuration);
      RGB.color(0, 0, 255);
    }
    else {
      updatePulse(Idle);
    }
  }
  else if (status == Released) {
    // TODO pulse is never blue, turns white / rose immediately
    ledTimer.changePeriod(3000);
    RGB.color(255, 0, 255);
  }
  else if (status == Idle) {
    ledTimer.stop();
    RGB.brightness(255);
    RGB.control(false);
  }
}

void KeyReleaseService::ledTimerCallback() {
  RGB.brightness(48);
  delay(100);
  RGB.brightness(0);
}

void KeyReleaseService::armKey(const int index) {
  servoControl[index].arm();
}

void KeyReleaseService::releaseKey(const int index) {
  servoControl[index].release();
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
      if (remainingMinutes == 0) {
        running = false;
      }
    }
  }
  return running;
}

const void KeyReleaseService::Duration::clear() {
  running = false;
  elapsedMinutes = 0;
  remainingMinutes = 0;
}


void KeyReleaseService::ServoControl::attach(const Actuator* actuator) {
  this->actuator = actuator;
  servo = new Servo();
#ifdef TEASELIB_KEYRELEASE_SERVO_DEACTIVATE_ON_IDLE
  /*
  delay(200);
  servo->write(actuator->armedAngle);
  delay(2000);
  servo->detach();
  delay(2000);
  servo->attach(actuator->pin);
  delay(200);
  servo->write(actuator->releasedAngle);
  delay(2000);
  servo->detach();
  */
  detachTimer = new Timer(2 * 1000, &ServoControl::detachCallback, *this, true);
#else
  servo->attach(actuator->pin);
#endif
}

void KeyReleaseService::ServoControl::arm() {
#ifdef TEASELIB_KEYRELEASE_SERVO_DEACTIVATE_ON_IDLE
   detachTimer->stop();
   servo->attach(actuator->pin);
   servo->write(actuator->armedAngle);
   detachTimer->start();
#else
  servo->write(actuator->armedAngle);
#endif
}

void KeyReleaseService::ServoControl::release() {
#ifdef TEASELIB_KEYRELEASE_SERVO_DEACTIVATE_ON_IDLE
  detachTimer->stop();
  servo->attach(actuator->pin);
  servo->write(actuator->releasedAngle);
  detachTimer->start();
#else
  servo->write(actuator->releasedAngle);
#endif
}

#ifdef TEASELIB_KEYRELEASE_SERVO_DEACTIVATE_ON_IDLE
  void KeyReleaseService::ServoControl::detachCallback() {
    servo->detach();
  }
#endif
