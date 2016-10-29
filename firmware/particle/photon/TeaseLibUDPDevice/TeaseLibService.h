#ifndef _INCLUDE_TeaseLibDeviceService
#define _INCLUDE_TeaseLibDeviceService

// Auto-include particle dev includes
#include <application.h>

#include "UDPMessage.h"

class TeaseLibService {
public:
  enum SleepMode {
    None,       // Dont't sleep at all - drains battery
    LightSleep, // Sleep but continue to run services
    DeepSleep   // Stop running services, reset on wakeup -> releases all keys
  };
  TeaseLibService(const char* const name, const char* const decription, const char* const version);
  const char* const name;
  const char* const description;
  const char* const version;
  static void setup(TeaseLibService** services, const int size, const char* deviceAddress);
  virtual bool canHandle(const char* serviceCommand) const;
  static const char* serviceCommand(const char* serviceCommand);
  static bool isCommand(const UDPMessage& received, const char* serviceCommand);
  static unsigned int processIdPacket(const UDPMessage& received, char* buffer);
  static unsigned int processSleepPacket(const UDPMessage& received, SleepMode& sleepMode);
  static unsigned int processPacket(const UDPMessage& received, char* buffer);
  static const UDPMessage Ok;
  static const char* const Id;
  static const char* const Sleep;
protected:
  virtual void setup()=0;
  virtual unsigned int process(const UDPMessage& received, char* buffer)=0;
  virtual unsigned int sleepRequested(const unsigned int requestedSleepDuration, SleepMode& sleepMode)=0;
private:
  static TeaseLibService** services;
  static unsigned int serviceCount;
  static const char* deviceAddress;
};

#endif /*end of include guard:   _INCLUDETeaseLibDeviceService */
