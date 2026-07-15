#include "stdafx.h"

#include <string>

#include "COMException.h"

#include "UnsupportedLanguageException.h"


const char* UnsupportedLanguageException::RuntimeClass = "teaselib/core/speechrecognition/UnsupportedLanguageException";

UnsupportedLanguageException::UnsupportedLanguageException(HRESULT hr)
	: NativeException(hr, COMException::Description(hr).c_str(), UnsupportedLanguageException::RuntimeClass)
{}

UnsupportedLanguageException::UnsupportedLanguageException(HRESULT hr, const std::wstring& message)
	: NativeException(hr, message.c_str(), UnsupportedLanguageException::RuntimeClass)
{}

UnsupportedLanguageException::~UnsupportedLanguageException()
{
}
