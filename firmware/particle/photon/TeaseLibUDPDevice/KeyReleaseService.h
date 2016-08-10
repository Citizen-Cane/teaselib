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

class KeyReleaseService : public TeaseLibService {
public:
  static const char* const Name;
  static const char* const Description;
  static const char* const Version;

  struct Actuator {
    const int pin;
    const int defaultMinutes;
    const int maximumMinutes;
    const int releasedAngle;
    const int armedAngle;
  };

  struct Duration {
      bool running;
      int elapsedMinutes;
      int remainingMinutes;

      const Actuator* actuator;
      const void arm();
      const int start(const int minutes);
      const int add(const int minutes);
      const bool advance();
      const void clear();
  };

  static const Actuator ShortRelease;
  static const Actuator LongRelease;
  static const Actuator* DefaultSetup[];
  static const int DefaultSetupSize;

  KeyReleaseService(const Actuator**, const int actuators);
  void setup();

  virtual int process(const UDPMessage& received, char* buffer);
private:
  const Actuator** actuators;
  Servo* servos;
  Duration* durations;
  const int actuatorCount;
  const char* const sessionKey;
  static const char* const createSessionKey();
  Timer releaseTimer;
  Timer ledTimer;
  void releaseTimerCallback();
  void ledTimerCallback();
  void armKey(const int index);
  void releaseKey(const int index);
  enum Status {
    Idle,
    Armed,
    Active,
    Released
  } status, lastStatus;
  void updatePulse();
  unsigned int nextRelease();
};

#endif /*end of include guard:   _INCLUDE_TeaseLibKeyReleaseService */
