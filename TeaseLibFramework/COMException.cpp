#include "StdAfx.h"

#include <sstream>
#include <iomanip>

#include <assert.h>

#include "ComException.h"


const char* COMException::RuntimeClass = "teaselib/core/jni/COMException";

COMException::COMException(HRESULT hr)
: NativeException(hr, FormatMessage(hr).c_str(), COMException::RuntimeClass) {
}

COMException::COMException(HRESULT hr, const wchar_t* message)
: NativeException(hr, message, COMException::RuntimeClass) {
}

std::wstring COMException::FormatMessage(HRESULT hr) {
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

	if (NULL != errorText) {
		std::wstring message(errorText);
		LocalFree(errorText);
		return message;
	}
	else {
		std::wstringstream message;
		message << L"COM-Error " << L"0x" << std::uppercase << std::setfill(L'0') << std::setw(4) << std::hex << hr;
		return message.str();
	}
}
