#include "StdAfx.h"

#include <assert.h>

#include "JNIException.h"
#include "JNIString.h"
#include "NativeException.h"

void JNIException::throwNew(JNIEnv* env, NativeException *e) {
	assert(!env->ExceptionCheck());
	jclass runtimeClass = env->FindClass(e->runtimeClass);
	JNIString message(env, e->message.c_str());
	jthrowable throwable = static_cast<jthrowable>(env->NewObject(
		runtimeClass,
		env->GetMethodID(runtimeClass, "<init>", "(ILjava/lang/String;)V"),
		e->errorCode,
		message.detach()));
	env->Throw(throwable);
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
