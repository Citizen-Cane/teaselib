#pragma once

#include <jni.h>

class JNIClass
{
public:
	JNIClass();
	virtual ~JNIClass();

	static jclass getClass(JNIEnv* env, const char* name);

	static jfieldID getFieldID(JNIEnv* env, jobject object, const char* name, const char* signature);
	static jfieldID getFieldID(JNIEnv* env, jclass clazz, const char* name, const char* signature);
	static jfieldID getFieldID(JNIEnv* env, const char* className, const char* name, const char* signature);

	static jfieldID getStaticFieldID(JNIEnv* env, jclass clazz, const char* name, const char* signature);

	static jmethodID getMethodID(JNIEnv* env, jobject object, const char* name, const char* signature);
	static jmethodID getMethodID(JNIEnv* env, jclass clazz, const char* name, const char* signature);
	static jmethodID getMethodID(JNIEnv* env, const char* className, const char* name, const char* signature);
};
