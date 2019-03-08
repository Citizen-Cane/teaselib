#include "StdAfx.h"

#include <assert.h>

#include "NativeException.h"

NativeException::NativeException(long errorCode, const std::wstring & message)
	: NativeException(errorCode, message.c_str())
{}

NativeException::NativeException(long errorCode, const wchar_t * const message)
: errorCode(errorCode)
, message(message)
{
	assert(false);
}
