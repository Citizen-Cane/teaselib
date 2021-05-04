#pragma once

#include "NativeException.h"

class TEASELIB_FRAMEWORK_EXPORT UnsupportedLanguageException : public NativeException {
public:
	UnsupportedLanguageException(HRESULT hr);
	UnsupportedLanguageException(HRESULT hr, const wchar_t* message);
	virtual ~UnsupportedLanguageException();
private:
	static const char* RuntimeClass;
};

