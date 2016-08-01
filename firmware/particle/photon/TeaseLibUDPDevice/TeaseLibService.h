#ifndef _INCLUDE_TeaseLibDeviceService
#define _INCLUDE_TeaseLibDeviceService

// Auto-include particle dev includes
#include <application.h>

#include "UDPMessage.h"

class TeaseLibService {
public:
  TeaseLibService(const char* const name, const char* const version);
  const char* const name;
  const char* const version;
  virtual void setup()=0;
  static void setup(TeaseLibService** services, const int size);
  virtual bool canHandle(const char* command);
  virtual int process(const UDPMessage& received, char* buffer)=0;
};

#endif /*end of include guard:   _INCLUDETeaseLibDeviceService */
