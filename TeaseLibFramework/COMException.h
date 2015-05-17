#pragma once

#include <string>

#include <atlbase.h>

#include "NativeException.h"

class COMException : public NativeException
{
public:
	COMException(HRESULT hr);
protected:
	static std::wstring FormatMessage(HRESULT hr);
};
