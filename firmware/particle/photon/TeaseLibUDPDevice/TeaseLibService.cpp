#include "string.h"

#include "TeaseLibService.h"

TeaseLibService::TeaseLibService(const char* const name, const char* const version)
: name(name), version(version) {
}

bool TeaseLibService::canHandle(const char* command) {
  return strncmp(name, command, strlen(name)) == 0;
}
