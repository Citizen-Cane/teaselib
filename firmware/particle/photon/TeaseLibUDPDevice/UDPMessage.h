#ifndef _INCLUDE_UDPMessage

#define _INCLUDE_UDPMessage

class UDPMessage {
public:
  UDPMessage(char* data);
  UDPMessage(const char* command, const char* parameter);
  UDPMessage(const char* command, const char** parameters, const int parameterCount);
  UDPMessage(const char* command, const char** parameters, const int parameterCount, const void* binary, const int binarySize);
  virtual ~UDPMessage();

  const char* command;
  const char** parameters;
  int parameterCount;
  const void* binary;
  int binarySize;
  char* buffer;
  int bufferSize;
private:
  char* packet(const char* command, const char** parameters, const int parameterCount, const void* binary, const int binarySize);
  const bool allocated;
};

#endif /* end of include guard:  _INCLUDE_UDPMessage
 */
