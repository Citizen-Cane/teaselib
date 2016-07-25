#ifndef _INCLUDE_UDPMessage
#define _INCLUDE_UDPMessage

class UDPMessage {
public:
  static int readShort(const char* data);
  static void writeShort(char* buffer, const int value);

  static bool isValid(const char* buffer, const int size, int startIndex);
  UDPMessage(char* data, const int size);
  UDPMessage(const char* command, const char** parameters, const int parameterCount);
  UDPMessage(const char* command, const char** parameters, const int parameterCount, const void* binary, const int binarySize);
  virtual ~UDPMessage();

  int toBuffer(char* buffer);

  const char* command;
  const char** parameters;
  int parameterCount;
  const void* binary;
  int binarySize;
private:
  static int sizeOf(const char* buffer);
  static int sizeOf(const char* command, const char** parameters, const int parameterCount);
  static int sizeOf(const char* command, const char** parameters, const int parameterCount, const int binarySize);
  const bool allocatedParameters;
};

#endif /* end of include guard:  _INCLUDE_UDPMessage */
