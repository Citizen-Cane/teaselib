#pragma once

#include <string>

class NativeException
{
public:
	NativeException(long errorCode, const std::wstring& message);
	NativeException(long errorCode, const wchar_t * const message);
	NativeException(long errorCode, const wchar_t* const message, const char* runtimeClass);

	const long errorCode;
	const std::wstring message;
	const char* runtimeClass;
};