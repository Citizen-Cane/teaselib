#ifndef _INCLUDE_TeaseLibDeviceService
#define _INCLUDE_TeaseLibDeviceService

#include "UDPMessage.h"

class TeaseLibService {
public:
  TeaseLibService(const char* const name, const char* const version);
  const char* const name;
  const char* const version;
  virtual bool canHandle(const char* command);
  virtual int process(const UDPMessage& received, char* buffer)=0;
};

#endif /*end of include guard:   _INCLUDETeaseLibDeviceService */
