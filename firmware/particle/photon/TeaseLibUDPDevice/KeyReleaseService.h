#ifndef _INCLUDE_TeaseLibKeyReleaseService
#define _INCLUDE_TeaseLibKeyReleaseService

#include "TeaseLibService.h"

class KeyReleaseService : public TeaseLibService {
public:
  static const char* const Name;
  static const char* const Version;
  KeyReleaseService();

  virtual int process(const UDPMessage& received, char* buffer);
};

#endif /*end of include guard:   _INCLUDE_TeaseLibKeyReleaseService */
