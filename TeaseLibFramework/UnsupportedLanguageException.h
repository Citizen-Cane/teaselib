#pragma once

#include "NativeException.h"

class UnsupportedLanguageException : public NativeException {
public:
	UnsupportedLanguageException(HRESULT hr);
	UnsupportedLanguageException(HRESULT hr, const wchar_t* message);
	virtual ~UnsupportedLanguageException();
private:
	static const char* RuntimeClass;
};

