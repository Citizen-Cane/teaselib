#pragma once

#include <jni.h>

#include "JNIObject.h"
#include "JNIString.h"

class NativeException;

class JNIException : public JNIObject<jthrowable>
{
public:
	static void throwNew(JNIEnv* env, NativeException *e);

	JNIException(JNIEnv *env);
	JNIException(JNIEnv *env, jthrowable throwable);
	JNIString getMessage() const;
};