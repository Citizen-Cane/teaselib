#include "StdAfx.h"

#include <assert.h>

#include "ComException.h"

COMException::COMException(HRESULT hr)
: NativeException(hr, FormatMessage(hr).c_str())
{
	assert(false);
}

std::wstring COMException::FormatMessage(HRESULT hr)
{
	LPTSTR errorText = NULL;
	::FormatMessage(
		// use system message tables to retrieve error text
		FORMAT_MESSAGE_FROM_SYSTEM
		// allocate buffer on local heap for error text
		| FORMAT_MESSAGE_ALLOCATE_BUFFER
		// Important! will fail otherwise, since we're not 
		// (and CANNOT) pass insertion parameters
		| FORMAT_MESSAGE_IGNORE_INSERTS,
		NULL,    // unused with FORMAT_MESSAGE_FROM_SYSTEM
		hr,
		MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
		(LPTSTR)&errorText,  // output 
		0, // minimum size for output buffer
		NULL);   // arguments - see note 
	if (NULL != errorText)
	{
		std::wstring message(errorText);
		LocalFree(errorText);
		return message;
	}
	else
	{
		return L"COM-Error";
	}
}
