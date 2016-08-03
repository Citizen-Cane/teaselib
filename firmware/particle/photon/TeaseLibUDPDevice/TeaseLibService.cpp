#include "string.h"

#include "TeaseLibService.h"


const UDPMessage TeaseLibService::Ok("ok", {}, 0);

TeaseLibService::TeaseLibService(const char* const name, const char* const description, const char* const version)
: name(name), description(description), version(version) {
}

void TeaseLibService::setup(TeaseLibService** services, const int size) {
  for(int i = 0; i < size; i++) {
    services[i]->setup();
  }
}

bool TeaseLibService::canHandle(const char* serviceCommand) const {
  return strncmp(name, serviceCommand, strlen(name)) == 0;
}

const char* TeaseLibService::serviceCommand(const char* serviceCommand) {
  return strchr(serviceCommand, ' ') + 1;
}

bool TeaseLibService::isCommand(const UDPMessage& received, const char* serviceCommand) {
  return strcmp(TeaseLibService::serviceCommand(received.command), serviceCommand) == 0;
}
