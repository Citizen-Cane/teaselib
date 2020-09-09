#include "StdAfx.h"

#include <exception>
#include <stdexcept>

#include <assert.h>

#include "JNIException.h"
#include "JNIString.h"
#include "NativeException.h"

void JNIException::rethrow(JNIEnv* env, std::invalid_argument& e)
{
	rethrow(env, e, "java/lang/IllegalArgumentException");
}

void JNIException::rethrow(JNIEnv* env, std::exception& e)
{
	rethrow(env, e, "java/lang/RuntimeException");
}

void JNIException::rethrow(JNIEnv* env, std::exception& e, const char* runtimeClass)
{
	assert(!env->ExceptionCheck());
	jclass jruntimeClass = env->FindClass(runtimeClass);
	JNIStringUTF8 message(env, e.what());

	jmethodID methodId = env->GetMethodID(jruntimeClass, "<init>", "(Ljava/lang/String;)V");
	assert(methodId);
	jthrowable throwable = static_cast<jthrowable>(env->NewObject(
		jruntimeClass,
		methodId,
		message.detach()));
	assert(throwable);
	env->Throw(throwable);
}

void JNIException::rethrow(JNIEnv* env, NativeException& e) {
	assert(!env->ExceptionCheck());
	jclass jruntimeClass = env->FindClass(e.runtimeClass);
	JNIString message(env, e.message.c_str());

	jmethodID methodId = env->GetMethodID(jruntimeClass, "<init>", "(ILjava/lang/String;)V");
	if (methodId != nullptr) {
		jthrowable throwable = static_cast<jthrowable>(env->NewObject(
			jruntimeClass,
			methodId,
			e.errorCode,
			message.detach()));
		assert(throwable);
		env->Throw(throwable);
	}
	else {
		jmethodID methodId = env->GetMethodID(jruntimeClass, "<init>", "(Ljava/lang/String;)V");
		assert(methodId);
		jthrowable throwable = static_cast<jthrowable>(env->NewObject(
			jruntimeClass,
			methodId,
			message.detach()));
		assert(throwable);
		env->Throw(throwable);
	}
}

JNIException::JNIException(JNIEnv *env)
: JNIException(env, env->ExceptionOccurred()) {
	env->ExceptionClear();
}

JNIException::JNIException(JNIEnv *env, jthrowable throwable)
: JNIObject(env, throwable) {
	env->ExceptionClear();
}

void JNIException::rethrow() const {
	env->Throw(jthis);
}

JNIString JNIException::getMessage() const {
	jclass runtimeClass = env->GetObjectClass(jthis);
	jstring message = static_cast<jstring>(env->CallObjectMethod(jthis, getMethodID("toString", "()Ljava/lang/String;")));
	assert(message);
	return JNIString(env, message);
}
