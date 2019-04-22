#pragma once

#include <NativeException.h>

class UnsupportedLanguageException : public NativeException {
public:
	UnsupportedLanguageException(HRESULT hr);
	virtual ~UnsupportedLanguageException();
private:
	static const char* RuntimeClass;
};

