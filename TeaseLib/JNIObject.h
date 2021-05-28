#pragma once

#include <assert.h>

#include <jni.h>

#include "JNIClass.h"

class TEASELIB_FRAMEWORK_EXPORT Objects {
public:
	static void requireNonNull(const wchar_t* name, jobject jobj);
	static void requireNonNull(const wchar_t* name, const void* jobj);
};

template<typename T> class JNIObject
{
public:
	JNIObject(JNIEnv *env, T jthis)
		: env(env), jthis(jthis)
	{
		assert(jthis);
	}

	virtual ~JNIObject()
	{
	}

	operator T() const
	{
		return jthis;
	}

	T detach()
	{
		T object = jthis;
		jthis = NULL;
		return object;
	}

protected:
	jmethodID getMethodID(const char* name, const char* signature) const
	{
		return JNIClass::getMethodID(env, jthis, name, signature);
	}

	JNIEnv *env;
	T jthis;
};