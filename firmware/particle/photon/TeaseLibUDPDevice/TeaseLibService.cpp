#include "string.h"

#include "TeaseLibService.h"

TeaseLibService::TeaseLibService(const char* const name, const char* const version)
: name(name), version(version) {
}

void TeaseLibService::setup(TeaseLibService** services, const int size) {
  for(int i = 0; i < size; i++) {
    services[i]->setup();
  }
}

bool TeaseLibService::canHandle(const char* command) {
  return strncmp(name, command, strlen(name)) == 0;
}
