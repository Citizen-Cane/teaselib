#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include<Particle.h>

#include "KeyReleaseService.h"

const char* const KeyReleaseService::Name = "KeyRelease";
const char* const KeyReleaseService::Description = "Servo-based key release mechanism";
const char* const KeyReleaseService::Version = "0.04";

/* Assembly:
 * From the Circuits folder, you'll need the following Fritzing sketches:
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
   Caution: When the battery drains and the voltage drops, moving the servos
   drop the voltage down below the amount needed to feed them with a proper PWM signal

   -> only operate the device with a usb power supply and use rechargeable batteries as backup power
   - Normal rechargeables last around two hours with activated WIFI and servos - test your own setup !
   So when you have a power outage in the middle of the session, you should still be able to get the espcape keys.


   Experimental (non-working, can be activated in source code):
   - To save battery charge servos are attached only for the time it takes to move the shaft to the requested angle.
   - This also resolves constant servo readjusting at low battery charge:
     - because the voltage drops when moving the servo, the PWM signal is not properly recognized by the servo anymore,
       resulting in constant readjusting, which drains the battery even more
*/

/* LED state:
  - The LED breathes cyan as usual (connected to the cloud) while the device is inactve.
  - Yellow pulse and an active hook indicates that the device is ready to receive a key
  - Green pulses indicate "Hold" mode - the device holds the key until the timer is started,
    or the Hold isn't renewed during the available duration
  - Dark blue pulse inditate that one or more timers count down - the frequency indicates the duration until the next release
  - Once all keys have been released, the device starts breathing cyan again
  the activity pulse frequence indicates the remaining time until all keys will have been released
*/

/* Sleep modes:
  + Deep sleep is supported if a single release is pending and
    the requested sleep time is greater than or equal to the relase duration
  + Otherwise the device enters light sleep until the next release.
  - Obviously the device doesn't react on commands while sleeping.
  TODO light sleep to all but last release, then switch to deep sleep until the last release is due.
  TODO Test with hold
  TODO Java RemoteDevice disconnects correctly, but fails to reconnect when device is available again
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

const unsigned int PulseFrequencyWhenIdle = 2000;
const unsigned int PulseFrequencyWhenArming = 1000;
const unsigned int PulseFrequencyWhenHolding = 2000;
const unsigned int PulseFrequencyWhenError = 500;
const unsigned int PulseLength = 100;

KeyReleaseService::KeyReleaseService(const Actuator** actuators, const unsigned int actuatorCount)
: TeaseLibService(Name, Description, Version)
, actuators(actuators)
, servoControl(new ServoControl[actuatorCount])
, durations(new Duration[actuatorCount])
, actuatorCount(actuatorCount)
, sessionKey(createSessionKey())
, releaseTimer(1 * 1000, &KeyReleaseService::releaseTimerCallback, *this)
, ledTimer(PulseFrequencyWhenIdle, &KeyReleaseService::ledTimerCallback, *this)
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
  for(unsigned int i = 0; i < actuatorCount; i++) {
      servoControl[i].attach(actuators[i]);
      durations[i].actuator = actuators[i];
      durations[i].clear(Actuator::Idle);
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
    const char count[2] = {static_cast<char>('0' + min(9UL, actuatorCount)), 0};
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
  else if (isCommand(received, "hold")) {
    // TODO When start() has been called, disallow hold() until release -> error
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    Duration& duration = durations[index];
    if (duration.status == Actuator::Armed || duration.status == Actuator::Holding) {
      duration.hold();
      if (duration.status == Actuator::Armed) {
        updatePulse(Actuator::Holding);
      }
      releaseTimer.start();
      const char* parameters[] = {sessionKey};
      return UDPMessage("releasekey", parameters, 1).toBuffer(buffer);
    } else {
      updatePulse(Actuator::Error);
	    releaseAllKeys();
      return WrongCall.toBuffer(buffer);
    }
  }
  else if (isCommand(received, "start" /* actuator seconds */)) {
    releaseTimer.stop();
    const int index = atol(received.parameters[0]);
    const int seconds = atol(received.parameters[1]);
    Duration& duration = durations[index];
    if (duration.status == Actuator::Armed || duration.status == Actuator::Holding) {
      if (seconds > 0) {
        duration.start(seconds);
        updatePulse(Actuator::CountDown);
        releaseTimer.start();
      } else {
        duration.hold();
        updatePulse(Actuator::AwaitRelease);
        releaseTimer.start();
      }
      const char* parameters[] = {sessionKey};
      return UDPMessage("releasekey", parameters, 1).toBuffer(buffer);
    } else {
      updatePulse(Actuator::Error);
	    releaseAllKeys();
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
    // TODO compare release key string in order to  prevent cheating
    Duration& duration = durations[index];
    duration.clear(Actuator::Released);
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
    case Actuator::CountDown: return "CountDown";
    case Actuator::AwaitRelease: return "AwaitRelease";
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
      pulseStatus = Actuator::CountDown;
    } else {
      pulseStatus = Actuator::Idle;
    }
  }
  // Update durations
  for(unsigned int i = 0; i < actuatorCount; i++) {
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

int KeyReleaseService::nextRelease() {
  int nextReleaseDuration = 0;
  for(unsigned int i = 0; i < actuatorCount; i++) {
    nextReleaseDuration = max(nextReleaseDuration, durations[i].remainingSeconds);
  }
  return nextReleaseDuration;
}

unsigned int KeyReleaseService::runningReleases() {
  unsigned int runningReleases = 0;
  for(unsigned int i = 0; i < actuatorCount; i++) {
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
    updatePulse(224, 128, 0, PulseFrequencyWhenArming);
  } else if (status == Actuator::Holding) {
    updatePulse(0, 192, 0, PulseFrequencyWhenHolding);
  } else if (status == Actuator::Error) {
    updatePulse(192, 0, 0, PulseFrequencyWhenError);
  } else if (status == Actuator::CountDown) {
    const unsigned int nextReleaseDuration = nextRelease();
    if (nextReleaseDuration > 0) {
      updatePulse(0, 0, 255, pulsePeriod(nextReleaseDuration));
    } else {
      updatePulse(Actuator::Idle);
    }
  } else if (status == Actuator::AwaitRelease) {
    updatePulse(0, 0, 192, PulseFrequencyWhenHolding);
  } else if (status == Actuator::Released) {
    updatePulse(255, 0, 255, PulseFrequencyWhenIdle);
  } else if (status == Actuator::Idle) {
    ledTimer.stop();
    RGB.brightness(255);
    RGB.control(false);
  }
}

void KeyReleaseService::updatePulse(const int r, const int g, const int b, const int periodMillis) {
  RGB.color(r, g, b);
  updatePulsePeriod(periodMillis);
}

void KeyReleaseService::updatePulsePeriod(const int periodMillis) {
  // Changing the period restarts the pulse timer, so don't update too often,
  // since the pulse is updated every second by the release timer
  if (secondsSinceLastUpdate++ * 1000 >= MaxPulseFrequency) {
    ledTimer.changePeriod(periodMillis);
    secondsSinceLastUpdate = 0;
  }
}

unsigned int KeyReleaseService::pulsePeriod(const unsigned int secondsUntilRelease) {
  const unsigned int pulseFrequency = PulseFrequencyAt0s + ((PulseFrequencyAt1h - PulseFrequencyAt0s) * secondsUntilRelease) / SecondsAt1h;
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

void KeyReleaseService::releaseAllKeys() {
	for (unsigned int index = 0; index < actuatorCount; index++) {
		Duration& duration = durations[index];
		duration.clear(Actuator::Idle);
		releaseKey(index);
	}
}

const void KeyReleaseService::Duration::arm() {
  running = true;
  elapsedSeconds = 0;
  remainingSeconds = actuator->defaultSeconds;
  status = Actuator::Armed;
}

const int KeyReleaseService::Duration::hold() {
  running = true;
  elapsedSeconds = 0;
  remainingSeconds = actuator->defaultSeconds;
  status = Actuator::Holding;
  return remainingSeconds;
}

const int KeyReleaseService::Duration::start(const int seconds) {
  running = true;
  elapsedSeconds = 0;
  remainingSeconds = min(seconds, actuator->maximumSeconds);
  status = Actuator::CountDown;
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
        clear(Actuator::Released);
      }
    }
  }
  return running;
}

const void KeyReleaseService::Duration::clear(const Actuator::Status status) {
  running = false;
  elapsedSeconds = 0;
  remainingSeconds = 0;
  this->status = status;
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
