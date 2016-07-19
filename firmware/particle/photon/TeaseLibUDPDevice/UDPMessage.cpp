#include "string.h"

#include "UDPMessage.h"

UDPMessage::UDPMessage(char* data) : buffer(data), bufferSize(0), allocated(false) {
  const char* reader = data;
  const int textPartSize = *reinterpret_cast<const short*>(reader);
  reader += 2;
  parameterCount = *reader++;
  command = reader;
  reader += strlen(reader) + 1;
  parameters = new const char*[parameterCount];
  for(int i = 0; i < parameterCount; i++) {
    parameters[i] = reader;
    reader += strlen(reader) + 1;
  }
  binarySize = *reinterpret_cast<const short*>(reader);
  reader += 2;
  if (binarySize > 0) {
    binary = reader;
  } else {
    binary = NULL;
  }
}

int sizeOf(const char* buffer) {
  const int textPartSize = *reinterpret_cast<const short*>(buffer);
  const int binarySize = *reinterpret_cast<const short*>(buffer[2 + textPartSize]);
  return 2 + textPartSize + 2 + binarySize;
}

int sizeOf(const char* command, const char** parameters, const int parameterCount) {
  int size = strlen(command) + 1;
  for(int i = 0; i < parameterCount; i++) {
    size += strlen(parameters[i]) + 1;
  }
  return size;
}

int sizeOf(const char* command, const char** parameters, const int parameterCount, const int binarySize) {
  return 2 + 1 + sizeOf(command, parameters, parameterCount) + 2 + binarySize;
}

UDPMessage::UDPMessage(const char* command, const char* parameter)
: buffer(packet(command, &parameter, 1, NULL, 0)), allocated(true) {
}

UDPMessage::UDPMessage(const char* command, const char** parameters, const int parameterCount)
: buffer(packet(command, parameters, parameterCount, NULL, 0)), allocated(true) {
}

UDPMessage::UDPMessage(const char* command, const char** parameters, const int parameterCount, const void* binary, const int binarySize)
: buffer(packet(command, parameters, parameterCount, binary, binarySize)), allocated(true) {
}

char* UDPMessage::packet(const char* command, const char** parameters, const int parameterCount, const void* binary, const int binarySize) {
  bufferSize = sizeOf(command, parameters, parameterCount, binarySize);
  char* buffer = new char[bufferSize];
  char* writer = buffer;
  *reinterpret_cast<short*>(writer) = sizeOf(command, parameters, parameterCount);
  writer += 2;
  strcpy(writer, command);
  writer += strlen(command);
  *writer++ = 0x0;
  for(int i = 0; i < parameterCount; i++) {
    strcpy(writer, parameters[i]);
    writer += strlen(parameters[i]);
    *writer++ = 0x0;
    *reinterpret_cast<short*>(writer) = binarySize;
    if (binarySize > 0) {
      memcpy(writer, binary, binarySize);
    }
  }
  return buffer;
}

UDPMessage::~UDPMessage() {
  if (allocated) {
    delete buffer;
    // responsibility of caller
    //for(int i = 0; i < parameterCount; i++) {
    //  delete parameters[i];
    //}
  }
  delete parameters;
}
