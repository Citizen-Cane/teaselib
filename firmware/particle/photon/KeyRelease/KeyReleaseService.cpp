#include "KeyReleaseService.h"

const char* const KeyReleaseService::Name = "KeyRelease";
const char* const KeyReleaseService::Description = "Servo-based key release mechanism";
const char* const KeyReleaseService::Version = "0.02";

/* Assembly:
 * From the Circuits folder, you'l need the following Fritzing sketches:
 * - a servo interface
 * - Vin Voltage Divider
 * Assemble all on bread- or perfboard. Both components match nicely together.
 * The source code matches the sketches, so you may have to adjust
 * - the servo angles
 * - the voltage threshold for each charge level (full, medium, low, empty)
 *
 * If you opt to use the passive sevo interface, the device has a slightly
 * higher power consumption, but is otherwsise fully functional.
 *
 * If charge level control, the device automatically releases all keys
 * when the battery level drops to "Low", and refuses to arm in order to protect your life.
 *
 * Without charge control, the device safety level is degraded from "HardwareFailure"
 * to "PowerFailure", since it is then your responsibility to recharge or replace the batteries.
 * - you have an increased likelihood that the device fails to release the keys.
 *
 * With charge control, the device can still fail to release the keys, plus you
 * might not be able to get the keys, but you can install a much more serious
 * emergency release because the odds you'll have to use it are much unlikely.
 *
 */

/* The default setup:
 - two servos, one for short term, one for long term self-bondage or restraint
 - servos open at an angle of 120°
   - 180° should be possible according to the Photon docs, but my servos seem to be to cheap to work that way
   - anyway 90° to 120° rotation is sufficient for holding and releasing the key
 - on startup the servos will turn down immediately release any keys
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
   - This also resolves constant servo readjusting at low battery charge:
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
  TODO light sleep to all but last release, then switch to deep sleep until the last release is due.
*/

const int KeyReleaseService::DefaultSetupSize = 2;
const int DefaultHours = 1;
const int ShortHours = 1;
const int LongHours = 2;
const KeyReleaseService::Actuator KeyReleaseService::ShortRelease = {TX, DefaultHours * 60 * 60, ShortHours * 60 * 60, 30, 150};
const KeyReleaseService::Actuator KeyReleaseService::LongRelease = {RX, DefaultHours * 60 * 60 , LongHours * 60 * 60, 150, 30};
const KeyReleaseService::Actuator* KeyReleaseService::DefaultSetup[] = {&ShortRelease, &LongRelease};

// timer period must be smaller than 65535 according to
// https://community.particle.io/t/scheduling-a-function-on-photon/18815/2
const unsigned int MaxPulseFrequency = 10000;
const unsigned int MinPulseFrequency = 250;


// pulse every 0.5s @ 0s -> 500ms
// pulse every 5.0s @ 3600s -> 1000ms
// y = mx+b -> m = (y-b) / x
// m = 5000ms - 500ms / 3600s ~ 1.25
const unsigned int PulseFrequencyAt0s = 500;
const unsigned int PulseFrequencyAt1h = 5000;
const unsigned int SecondsAt1h = 3600;

const unsigned int PulseFrequencyWhenIdleOrArming = 1000;
const unsigned int PulseLength = 100;

KeyReleaseService::KeyReleaseService(const Actuator** actuators, const int actuatorCount)
: TeaseLibService(Name, Description, Version)
, actuators(actuators)
, servoControl(new ServoControl[actuatorCount])
, durations(new Duration[actuatorCount])
, actuatorCount(actuatorCount)
, sessionKey(createSessionKey())
, releaseTimer(1 * 1000, &KeyReleaseService::releaseTimerCallback, *this)
, ledTimer(PulseFrequencyWhenIdleOrArming, &KeyReleaseService::ledTimerCallback, *this)
, ledPulseOffTimer(PulseLength, &KeyReleaseService::ledTimerPulseOffCallback, *this, true)
, pulseStatus(Actuator::Idle)
, secondsSinceLastUpdate(0)
{}

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
      Serial.print(durations[i].actuator->defaultSeconds, DEC);
      Serial.print("s, maximum=");
      Serial.print(durations[i].actuator->maximumSeconds, DEC);
      Serial.println("s");
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
    updatePulse(Actuator::Armed);
    armKey(index);
    releaseTimer.start();
    return Ok.toBuffer(buffer);
  }
  else if (isCommand(received, "hold" /* actuator seconds */)) {
    // TODO When start() has been called, disallow hold() until release -> error
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    const int seconds = atol(received.parameters[1]);
    Duration& duration = durations[index];
    if (duration.status == Actuator::Armed || duration.status == Actuator::Holding) {
      duration.hold(seconds);
      updatePulse(Actuator::Holding);
      releaseTimer.start();
      const char* parameters[] = {sessionKey};
      return UDPMessage("releasekey", parameters, 1).toBuffer(buffer);
    } else {
      updatePulse(Actuator::Error);
      releaseKey(index);
      Duration& duration = durations[index];
      duration.clear();
      releaseKey(index);
      return WrongCall.toBuffer(buffer);
    }
  }
  else if (isCommand(received, "start" /* actuator seconds */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    const int seconds = atol(received.parameters[1]);
    Duration& duration = durations[index];
    if (duration.status == Actuator::Armed || duration.status == Actuator::Holding) {
      duration.start(seconds);
      updatePulse(Actuator::Active);
      releaseTimer.start();
      const char* parameters[] = {sessionKey};
      return UDPMessage("releasekey", parameters, 1).toBuffer(buffer);
    } else {
      updatePulse(Actuator::Error);
      releaseKey(index);
      Duration& duration = durations[index];
      duration.clear();
      releaseKey(index);
      return WrongCall.toBuffer(buffer);
    }
  }
  else if (isCommand(received, "add" /* actuator seconds */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    const int seconds = atol(received.parameters[1]);
    Duration& duration = durations[index];
    duration.add(seconds);
    updatePulse(pulseStatus);
    releaseTimer.start();
    return createCountMessage(duration.remainingSeconds, buffer);
  }
  else if (isCommand(received, "available" /* actuator */)) {
    const int index = atol(received.parameters[0]);
    Duration& duration = durations[index];
    return createCountMessage(duration.actuator->maximumSeconds - duration.elapsedSeconds, buffer);
  }
  else if (isCommand(received, "remaining" /* actuator */)) {
    const int index = atol(received.parameters[0]);
    Duration& duration = durations[index];
    return createCountMessage(duration.remainingSeconds, buffer);
  }
  else if (isCommand(received, "running" /* actuator */)) {
    // TODO Deprecate and use status instead
    const int index = atol(received.parameters[0]);
    Duration& duration = durations[index];
    return createCountMessage(duration.running ? 1 : 0, buffer);
  }
  else if (isCommand(received, "status" /* actuator */)) {
    const int index = atol(received.parameters[0]);
    Duration& duration = durations[index];
    const char* parameters[] = {Actuator::statusText(duration.status)};
    return UDPMessage(TeaseLibService::Text, parameters, 1).toBuffer(buffer);
  }
  else if (isCommand(received, "release" /* actuator releaseKey */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    // TODO comparre release key string in order to  prevent cheating
    Duration& duration = durations[index];
    duration.clear();
    updatePulse(Actuator::Released);
    releaseKey(index);
    releaseTimer.start();
    return Ok.toBuffer(buffer);
  }
  else  {
    return 0;
  }
}

const char* KeyReleaseService::Actuator::statusText(Status status) {
  switch((int) status) {
    case Actuator::Idle: return "Idle";
    case Actuator::Armed: return "Armed";
    case Actuator::Holding: return "Holding";
    case Actuator::Error: return "Error";
    case Actuator::Active: return "Active";
    case Actuator::Released: return "Released";
    default: return "Unknown";
  }
}

unsigned int KeyReleaseService::createCountMessage(unsigned int count, char* buffer) {
  char digits[8];
  sprintf(digits, "%d", count);
  const char* parameters[] = {digits};
  return UDPMessage("count", parameters, 1).toBuffer(buffer);
}


unsigned int KeyReleaseService::sleepRequested(const unsigned int durationSeconds, SleepMode& sleepMode) {
  const unsigned int n = runningReleases();
  if (n == 0) {
    return durationSeconds;
  } else if (n == 1) {
    const unsigned int nextReleaseDuration = nextRelease();
    if (sleepMode == DeepSleep && nextReleaseDuration <= durationSeconds) {
      return nextReleaseDuration;
    } else {
      sleepMode = LightSleep;
      return durationSeconds;
    }
  } else {
      const unsigned int nextReleaseDuration = nextRelease();
      sleepMode = LightSleep;
      return nextReleaseDuration;
  }
}

void KeyReleaseService::releaseTimerCallback() {
  // Resume to Active or Idle after one minute
  if (pulseStatus == Actuator::Released) {
    const unsigned int nextReleaseDuration = nextRelease();
    if (nextReleaseDuration > 0) {
      pulseStatus = Actuator::Active;
    } else {
      pulseStatus = Actuator::Idle;
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
        updatePulse(Actuator::Released);
      }
    }
  }
  // Update pulse
  if (pulseStatus != Actuator::Released) {
    updatePulse(pulseStatus);
  }
}

unsigned int KeyReleaseService::nextRelease() {
  unsigned int nextReleaseDuration = 0;
  for(int i = 0; i < actuatorCount; i++) {
    nextReleaseDuration = max(nextReleaseDuration, durations[i].remainingSeconds);
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

void KeyReleaseService::updatePulse(const Actuator::Status status) {
  if (this->pulseStatus != status) {
    this->pulseStatus = status;
    secondsSinceLastUpdate = MaxPulseFrequency / 1000;
  }

  // TODO On state change, light up the new color for a second or so before flashing
  // to indicate immediately that a change has been applied

  if (status == Actuator::Armed) {
    RGB.control(true);
    RGB.color(224, 128, 0);
    updatePulse(PulseFrequencyWhenIdleOrArming);
  } else if (status == Actuator::Holding) {
    const unsigned int nextReleaseDuration = nextRelease();
    RGB.color(0, 192, 0);
    updatePulse(PulseFrequencyWhenIdleOrArming);
  } else if (status == Actuator::Error) {
    const unsigned int nextReleaseDuration = nextRelease();
    RGB.color(192, 0, 0);
    updatePulse(PulseFrequencyWhenIdleOrArming);
  } else if (status == Actuator::Active) {
    const unsigned int nextReleaseDuration = nextRelease();
    if (nextReleaseDuration > 0) {
      RGB.color(0, 0, 255);
      updatePulse(pulsePeriod(nextReleaseDuration));
    } else {
      updatePulse(Actuator::Idle);
    }
  } else if (status == Actuator::Released) {
    RGB.color(255, 0, 255);
    updatePulse(PulseFrequencyWhenIdleOrArming);
  } else if (status == Actuator::Idle) {
    ledTimer.stop();
    RGB.brightness(255);
    RGB.control(false);
  }
}

void KeyReleaseService::updatePulse(const int frequencyMillis) {
  // Changing the period restarts the pulse timer, so don't update too often,
  // since the pulse is updated every second by the release timer
  if (secondsSinceLastUpdate++ * 1000 >= MaxPulseFrequency) {
    ledTimer.changePeriod(frequencyMillis);
    secondsSinceLastUpdate = 0;
  }
}

unsigned int KeyReleaseService::pulsePeriod(const unsigned int seconds) {
  const unsigned int pulseFrequency = PulseFrequencyAt0s + ((PulseFrequencyAt1h - PulseFrequencyAt0s) * seconds) / SecondsAt1h;
  const unsigned int lowPulseFrequency = max(pulseFrequency, MinPulseFrequency);
  return min(lowPulseFrequency, MaxPulseFrequency);
}

void KeyReleaseService::ledTimerCallback() {
  RGB.brightness(48);
  ledPulseOffTimer.start();
}

void KeyReleaseService::ledTimerPulseOffCallback() {
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
  elapsedSeconds = 0;
  remainingSeconds = actuator->defaultSeconds;
  status = Actuator::Armed;
}

// TODO Remove unused argument "seconds"
const int KeyReleaseService::Duration::hold(const int seconds) {
  elapsedSeconds = 0;
  remainingSeconds = actuator->defaultSeconds;
  status = Actuator::Holding;
  return remainingSeconds;
}

const int KeyReleaseService::Duration::start(const int seconds) {
  remainingSeconds = min(seconds, actuator->maximumSeconds - elapsedSeconds);
  status = Actuator::Active;
  return remainingSeconds;
}

const int KeyReleaseService::Duration::add(const int seconds) {
  remainingSeconds = min(remainingSeconds + seconds, actuator->maximumSeconds - elapsedSeconds);
  return remainingSeconds;
}

const bool KeyReleaseService::Duration::advance() {
  if (running) {
    if (remainingSeconds > 0 && elapsedSeconds < actuator->maximumSeconds) {
      elapsedSeconds++;
      remainingSeconds--;
      if (remainingSeconds == 0) {
        running = false;
        status = Actuator::Released;
      }
    }
  }
  return running;
}

const void KeyReleaseService::Duration::clear() {
  running = false;
  elapsedSeconds = 0;
  remainingSeconds = 0;
  status = Actuator::Idle;
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
