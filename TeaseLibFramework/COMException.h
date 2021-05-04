#pragma once

#include <string>

#include <atlbase.h>

#include "NativeException.h"

class TEASELIB_FRAMEWORK_EXPORT COMException : public NativeException {
public:
	COMException(HRESULT hr);
	COMException(HRESULT hr, const wchar_t* message);
	static std::wstring Description(HRESULT hr);
protected:
private:
	static const char* RuntimeClass;
};
