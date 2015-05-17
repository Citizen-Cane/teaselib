#pragma once

#include "JNIObject.h"

class JNIString : public JNIObject<jstring>
{
public:
	JNIString(JNIEnv *env, jstring string);
	JNIString(JNIEnv *env, const wchar_t * const string);
	virtual ~JNIString();
	operator const wchar_t*() const;
private:
	const wchar_t *string;
};

