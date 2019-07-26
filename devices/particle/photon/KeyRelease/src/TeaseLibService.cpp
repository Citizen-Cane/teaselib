#include <stdlib.h>
#include <string.h>

#include "TeaseLibService.h"


const UDPMessage TeaseLibService::Ok("ok", {}, 0);
const UDPMessage TeaseLibService::WrongCall("WrongCall", {}, 0);
const char* const TeaseLibService::Id = "id";
const char* const TeaseLibService::Sleep = "sleep";
const char* const TeaseLibService::Text = "text";

TeaseLibService** TeaseLibService::services = NULL;
unsigned int TeaseLibService::serviceCount = 0;
const char* TeaseLibService::deviceAddress = NULL;

TeaseLibService::TeaseLibService(const char* const name, const char* const description, const char* const version)
: name(name), description(description), version(version) {
}

void TeaseLibService::setup(TeaseLibService** services, const int size, const char* deviceAddress) {
  TeaseLibService::services = services;
  TeaseLibService::serviceCount = size;
  TeaseLibService::deviceAddress = deviceAddress;
  for(unsigned int i = 0; i < TeaseLibService::serviceCount ; i++) {
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

unsigned int TeaseLibService::processIdPacket(char* buffer) {
  const char* deviceId = "My_Photon";
  const char* parameters[] = {deviceId, deviceAddress, "1", services[0]->name, services[0]->description, services[0]->version};
  return UDPMessage("services", parameters, sizeof(parameters)/sizeof(char*)).toBuffer(buffer);
}

unsigned int TeaseLibService::processSleepPacket(const UDPMessage& received, SleepMode& sleepMode) {
  unsigned int durationSeconds = atol(received.parameters[0]);
  for(unsigned int i = 0; i < TeaseLibService::serviceCount ; i++) {
    durationSeconds = TeaseLibService::services[i]->sleepRequested(durationSeconds, sleepMode);
    if (sleepMode == None) {
      durationSeconds = 0;
      break;
    }
  }
  return durationSeconds;
}

unsigned int TeaseLibService::processPacket(const UDPMessage& received, char* buffer) {
  for(unsigned int i = 0; i < serviceCount; i++) {
    if (services[i]->canHandle(received.command)) {
      return services[i]->process(received, buffer);
    }
  }
  return 0;
}
