#include "string.h"

#include "UDPMessage.h"


int UDPMessage::readShort(const char* data) {
    return 256 * data[0] + data[1];
}

void UDPMessage::writeShort(char* buffer, const int value) {
  buffer[0] = (value & 0xffff0000) >> 8;
  buffer[1] = (value & 0x0000ffff);
}

int UDPMessage::sizeOf(const char* buffer) {
  const int textPartSize = readShort(buffer);
  const int binarySize = readShort(&buffer[2 + textPartSize]);
  return 2 + textPartSize + 2 + binarySize;
}

int UDPMessage::sizeOf(const char* command, const char** parameters, const int parameterCount) {
  int size = strlen(command) + 1;
  for(int i = 0; i < parameterCount; i++) {
    size += strlen(parameters[i]) + 1;
  }
  return size;
}

int UDPMessage::sizeOf(const char* command, const char** parameters, const int parameterCount, const int binarySize) {
  return 2 + 1 + sizeOf(command, parameters, parameterCount) + 2 + binarySize;
}

bool UDPMessage::isValid(const char* data, const int size, int startIndex) {
  int offset = 0;
  int textSize = readShort(&data[startIndex + offset]);
  offset +=2;
  int parameterCount = data[startIndex + offset++];
  int commandNameLength = strlen(&data[startIndex + offset]);
  offset += commandNameLength + 1;
  for (int i = 0; i < parameterCount; i++) {
      int parameterLength = strlen(&data[startIndex + offset]);
      offset += parameterLength + 1;
  }
  if (offset - 2 != textSize) {
      return false;
  }
  if (size >= offset + 2) {
      int binarySize = readShort(&data[startIndex + offset]);
      offset +=2;
      if (size < offset + binarySize) {
          return false;
      }
      offset += binarySize;
  }
  return offset == size;
}

UDPMessage::UDPMessage(char* data, const int size)
: allocatedParameters(true) {
  const char* reader = data;
  const int textPartSize = readShort(reader);
  reader += 2;
  parameterCount = *reader++;
  command = reader;
  reader += strlen(reader) + 1;
  parameters = new const char*[parameterCount];
  for(int i = 0; i < parameterCount; i++) {
    parameters[i] = reader;
    reader += strlen(reader) + 1;
  }
  binarySize = readShort(reader);
  reader += 2;
  if (binarySize > 0) {
    binary = reader;
  } else {
    binary = NULL;
  }
}

UDPMessage::UDPMessage(const char* command, const char** parameters, const int parameterCount)
: command(command), parameters(parameters), parameterCount(parameterCount), binary(NULL), binarySize(0), allocatedParameters(false) {
}

UDPMessage::UDPMessage(const char* command, const char** parameters, const int parameterCount, const void* binary, const int binarySize)
: command(command), parameters(parameters), parameterCount(parameterCount), binary(binary), binarySize(binarySize), allocatedParameters(false)  {
}

int UDPMessage::toBuffer(char* buffer) {
  int index = 0;
  writeShort(buffer + index, sizeOf(command, parameters, parameterCount));
  index += 2;
  strcpy(buffer + index, command);
  index += strlen(command);
  buffer[index++] = 0x0;
  for(int i = 0; i < parameterCount; i++) {
    strcpy(buffer + index, parameters[i]);
    index += strlen(parameters[i]);
    buffer[index++] = 0x0;
    writeShort(buffer + index, binarySize);
    index += 2;
    if (binarySize > 0) {
      memcpy(buffer + index, binary, binarySize);
      index += binarySize;
    }
  }
  return index;
}

UDPMessage::~UDPMessage() {
  if (allocatedParameters) {
    delete parameters;
  }
}
