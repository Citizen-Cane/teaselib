#ifndef _INCLUDE_TeaseLibKeyReleaseService
#define _INCLUDE_TeaseLibKeyReleaseService

#include "TeaseLibService.h"

/* Usage instructions:
 * call "arm" the device to rotate the servo up
 * -> release timer is started to ensure release happens at least after the default time
 * call "start" to run a timer with the specified time
 * -> the servo rotates down and realeases the key automatically when the default duration has elapsed
 * call "release" to release the key early (provided you have the release key)
 * call "add" to increase the duration up to the maximum allowed duration
 * - book-keeping ensures that the maximum duration cannot be exceeded
 */

// enable experimental servo detach
//#define TEASELIB_KEYRELEASE_SERVO_DEACTIVATE_ON_IDLE

class KeyReleaseService : public TeaseLibService {
public:
  static const char* const Name;
  static const char* const Description;
  static const char* const Version;

  struct Actuator {
    const int pin;
    const int defaultSeconds;
    const int maximumSeconds;
    const int releasedAngle;
    const int armedAngle;
  };

  struct Duration {
      bool running;
      int elapsedSeconds;
      int remainingSeconds;

      const Actuator* actuator;
      const void arm();
      const int start(const int seconds);
      const int add(const int seconds);
      const bool advance();
      const void clear();
  };

  class ServoControl {
  public:
    void attach(const Actuator* actuator);
    void arm();
    void release();
private:
    const Actuator* actuator;
    Servo* servo;
#ifdef TEASELIB_KEYRELEASE_SERVO_DEACTIVATE_ON_IDLE
     Timer* detachTimer;
    void detachCallback();
#endif
  };

  static const Actuator ShortRelease;
  static const Actuator LongRelease;
  static const Actuator* DefaultSetup[];
  static const int DefaultSetupSize;

  KeyReleaseService(const Actuator**, const int actuators);
protected:
  virtual void setup();
  virtual unsigned int process(const UDPMessage& received, char* buffer);
  virtual unsigned int sleepRequested(const unsigned int requestedSleepDuration, SleepMode& sleepMode);
private:
  const Actuator** actuators;
  ServoControl* servoControl;
  Duration* durations;
  const int actuatorCount;
  const char* const sessionKey;
  static const char* const createSessionKey();

  Timer releaseTimer;
  Timer ledTimer;
  Timer ledPulseOffTimer;
  void releaseTimerCallback();
  void ledTimerCallback();
  void ledTimerPulseOffCallback();
  unsigned int pulsePeriod(const unsigned int seconds);

  void armKey(const int index);
  void releaseKey(const int index);
  enum Status {
    Idle,
    Armed,
    Active,
    Released
  } status;
  void updatePulse(Status status);
  unsigned int nextRelease();
  unsigned int runningReleases();
  unsigned int createCountMessage(unsigned int count, char* buffer);
};

#endif /*end of include guard:   _INCLUDE_TeaseLibKeyReleaseService */
