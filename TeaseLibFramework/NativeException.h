#pragma once

#include <string>

class NativeException
{
public:
	NativeException(long errorCode, const std::wstring& message);
	NativeException(long errorCode, const wchar_t * const message);
	long errorCode;
	std::wstring message;
};