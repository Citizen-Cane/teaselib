#include "StdAfx.h"

#include <assert.h>

#include "JNIException.h"
#include "JNIString.h"
#include "NativeException.h"

void JNIException::throwNew(JNIEnv* env, NativeException& e) {
	assert(!env->ExceptionCheck());
	jclass runtimeClass = env->FindClass(e.runtimeClass);
	JNIString message(env, e.message.c_str());

	jmethodID methodId = env->GetMethodID(runtimeClass, "<init>", "(ILjava/lang/String;)V");
	if (methodId != nullptr) {
		jthrowable throwable = static_cast<jthrowable>(env->NewObject(
			runtimeClass,
			methodId,
			e.errorCode,
			message.detach()));
		assert(throwable);
		env->Throw(throwable);
	}
	else {
		jmethodID methodId = env->GetMethodID(runtimeClass, "<init>", "(Ljava/lang/String;)V");
		assert(methodId);
		jthrowable throwable = static_cast<jthrowable>(env->NewObject(
			runtimeClass,
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
