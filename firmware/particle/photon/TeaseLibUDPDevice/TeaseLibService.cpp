#include "string.h"

#include "TeaseLibService.h"


const UDPMessage TeaseLibService::Ok("ok", {}, 0);
const char* const TeaseLibService::Id = "id";

TeaseLibService** TeaseLibService::services = NULL;
int TeaseLibService::serviceCount = 0;

TeaseLibService::TeaseLibService(const char* const name, const char* const description, const char* const version)
: name(name), description(description), version(version) {
}

void TeaseLibService::setup(TeaseLibService** services, const int size) {
  TeaseLibService::services = services;
  TeaseLibService::serviceCount = size;
  for(int i = 0; i < TeaseLibService::serviceCount ; i++) {
    TeaseLibService::services[i]->setup();
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

int TeaseLibService::processIdPacket(const UDPMessage& received, char* buffer) {
  const String deviceId = "My Photon";
  const char* parameters[] = {deviceId, "1", services[0]->name,  services[0]->description, services[0]->version};
  return UDPMessage("services", parameters, sizeof(parameters)/sizeof(char*)).toBuffer(buffer);
}
