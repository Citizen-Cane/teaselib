#pragma once

#include <exception>
#include <stdexcept>

#include <jni.h>

#include "JNIObject.h"
#include "JNIString.h"

class NativeException;

class TEASELIB_FRAMEWORK_EXPORT JNIException : public JNIObject<jthrowable>
{
public:
	static void rethrow(JNIEnv* env, const std::invalid_argument& e);
	static void rethrow(JNIEnv* env, const std::exception& e);
	static void rethrow(JNIEnv* env, const std::exception& e, const char* runtimeClass);
	static void rethrow(JNIEnv* env, const char* message, const char* runtimeClass);
	static void rethrow(JNIEnv* env, const NativeException& e);
	
	JNIException(JNIEnv *env);
	JNIException(JNIEnv* env, jthrowable throwable);
	void rethrow() const;
	JNIString getMessage() const;
	void printStacktrace() const;
};