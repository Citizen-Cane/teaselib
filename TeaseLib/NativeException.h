#pragma once

#include <string>

class TEASELIB_FRAMEWORK_EXPORT NativeException
{
public:
	NativeException(long errorCode, const std::wstring& message);
	NativeException(long errorCode, const wchar_t * const message);
	NativeException(long errorCode, const wchar_t* const message, const char* runtimeClass);
	virtual ~NativeException();

	const long errorCode;
	const wchar_t* message;
	const char* runtimeClass;
};