#ifndef _INCLUDE_TeaseLibDeviceService
#define _INCLUDE_TeaseLibDeviceService

// Auto-include particle dev includes
#include <application.h>

#include "UDPMessage.h"

class TeaseLibService {
public:
  TeaseLibService(const char* const name, const char* const decription, const char* const version);
  const char* const name;
  const char* const description;
  const char* const version;
  virtual void setup()=0;
  static void setup(TeaseLibService** services, const int size);
  virtual bool canHandle(const char* serviceCommand) const;
  static const char* serviceCommand(const char* serviceCommand);
  static bool isCommand(const UDPMessage& received, const char* serviceCommand);
  static int processIdPacket(const UDPMessage& received, char* buffer);
  virtual int process(const UDPMessage& received, char* buffer)=0;
  static const UDPMessage Ok;
  static const char* const Id;
private:
  static TeaseLibService** services;
  static int serviceCount;
};

#endif /*end of include guard:   _INCLUDETeaseLibDeviceService */
