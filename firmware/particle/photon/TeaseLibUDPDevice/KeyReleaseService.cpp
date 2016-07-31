#include "KeyReleaseService.h"

const char* const KeyReleaseService::Name = "KeyRelease";
const char* const KeyReleaseService::Version = "0.01";

KeyReleaseService::KeyReleaseService()
: TeaseLibService(Name, Version) {
}

int KeyReleaseService::process(const UDPMessage& received, char* buffer) {
  const char* parameters[] = {"2"};
  return UDPMessage("actuators", parameters, 1).toBuffer(buffer);
}
