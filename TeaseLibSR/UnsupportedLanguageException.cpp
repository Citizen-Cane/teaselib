#include "stdafx.h"

#include<COMException.h>

#include "UnsupportedLanguageException.h"


const char* UnsupportedLanguageException::RuntimeClass = "teaselib/core/speechrecognition/implementation/UnsupportedLanguageException";

UnsupportedLanguageException::UnsupportedLanguageException(HRESULT hr)
	: NativeException(hr, COMException::FormatMessage(hr).c_str(), UnsupportedLanguageException::RuntimeClass)
{}

UnsupportedLanguageException::UnsupportedLanguageException(HRESULT hr, const wchar_t* message)
	: NativeException(hr, message, UnsupportedLanguageException::RuntimeClass)
{}

UnsupportedLanguageException::~UnsupportedLanguageException()
{
}
