#pragma once

#include <string>

#include <atlbase.h>

#include "NativeException.h"

class COMException : public NativeException {
public:
	COMException(HRESULT hr);
	COMException(HRESULT hr, const wchar_t* message);
	static std::wstring Description(HRESULT hr);
protected:
private:
	static const char* RuntimeClass;
};
