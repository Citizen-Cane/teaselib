#include "StdAfx.h"

#include <sstream>
#include <iomanip>

#include <assert.h>

#include "ComException.h"


const char* COMException::RuntimeClass = "teaselib/core/jni/COMException";

COMException::COMException(HRESULT hr)
: NativeException(hr, Description(hr).c_str(), COMException::RuntimeClass) {
}

COMException::COMException(HRESULT hr, const wchar_t* message)
: NativeException(hr, message, COMException::RuntimeClass) {
}

std::wstring COMException::Description(HRESULT hr) {
	CComPtr<IErrorInfo> errorInfo;
	if (S_OK == ::GetErrorInfo(0, &errorInfo)) {
		BSTR description = nullptr;
		if (S_OK == errorInfo->GetDescription(&description)) {
			const std::wstring message = description;
			SysFreeString(description);
			return message;
		}
	}
	
	// Fallback
	TCHAR errorText[500];
	if (::FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS, NULL,
		hr, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), errorText, 500, NULL)) {
		return errorText;
	}
	else {
		std::wstringstream message;
		message << L"COM-Error " << L"0x" << std::uppercase << std::setfill(L'0') << std::setw(4) << std::hex << hr;
		return message.str();
	}
}
