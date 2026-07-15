#pragma once

#include <string>

#include "NativeException.h"

class TEASELIB_FRAMEWORK_EXPORT UnsupportedLanguageException : public NativeException {
public:
	UnsupportedLanguageException(HRESULT hr);
	UnsupportedLanguageException(HRESULT hr, const std::wstring& message);
	virtual ~UnsupportedLanguageException();
private:
	static const char* RuntimeClass;
};

