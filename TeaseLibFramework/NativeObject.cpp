#include "stdafx.h"

#include <assert.h>

#include <jni.h>

#include "NativeObject.h"
#include "teaselib_util_jni_NativeObject.h"

// TODO Should be compiled into the framework, but I couldn't find out
// how to make it publically accessible in the actual libraries
// -> #included by ${ProjectName).cpp for now

extern "C"
{
	/*
	* Class:     teaselib_util_jni_NativeObject
	* Method:    disposeNativeObject
	* Signature: ()V
	*/
	JNIEXPORT void JNICALL Java_teaselib_util_jni_NativeObject_disposeNativeObject
		(JNIEnv *env, jobject jthis)
	{
		NativeObject::dispose(env, jthis);
	}
}
