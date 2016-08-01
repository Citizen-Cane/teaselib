#ifndef _INCLUDE_TeaseLibKeyReleaseService
#define _INCLUDE_TeaseLibKeyReleaseService

#include "TeaseLibService.h"

class KeyReleaseService : public TeaseLibService {
public:
  static const char* const Name;
  static const char* const Version;

  struct Actuator {
    const int pin;
    const int defaultDuration;
  };

  static const Actuator ShortRelease;
  static const Actuator LongRelease;
  static const Actuator* DefaultSetup[];
  static const int DefaultSetupSize;

  KeyReleaseService(const Actuator**, const int actuators);
  void setup();

  virtual int process(const UDPMessage& received, char* buffer);
};

#endif /*end of include guard:   _INCLUDE_TeaseLibKeyReleaseService */
