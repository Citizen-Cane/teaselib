#include "StdAfx.h"

#include <assert.h>

#include "NativeException.h"

NativeException::NativeException(const long errorCode, const std::wstring & message)
	: NativeException(errorCode, message.c_str()) 
{}

NativeException::NativeException(const long errorCode, const wchar_t* message)
	: NativeException(errorCode, message, "teaselib/core/jni/NativeException")
{}

NativeException::NativeException(long errorCode, const wchar_t* const message, const char* runtimeClass)
	: errorCode(errorCode)
	, message(_wcsdup(message))
	, runtimeClass(runtimeClass)
{
}

NativeException::~NativeException()
{
	delete message;
}
