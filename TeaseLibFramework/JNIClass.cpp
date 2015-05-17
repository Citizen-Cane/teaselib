#include "stdafx.h"

#include "JNIClass.h"
#include "JNIException.h"


JNIClass::JNIClass()
{
}

JNIClass::~JNIClass()
{
}

jclass JNIClass::getClass(JNIEnv* env, const char* name)
{
	jclass clazz = env->FindClass(name);
	assert(!env->ExceptionCheck());
	if (env->ExceptionCheck()) throw new JNIException(env);
	return clazz;
}

jfieldID JNIClass::getFieldID(JNIEnv* env, jobject object, const char* name, const char* signature)
{
	jfieldID id = env->GetFieldID(env->GetObjectClass(object), name, signature);
	assert(!env->ExceptionCheck());
	if (env->ExceptionCheck()) throw new JNIException(env);
	return id;
}

jfieldID JNIClass::getFieldID(JNIEnv* env, jclass clazz, const char* name, const char* signature)
{
	jfieldID id = env->GetFieldID(clazz, name, signature);
	assert(!env->ExceptionCheck());
	if (env->ExceptionCheck()) throw new JNIException(env);
	return id;
}

jfieldID JNIClass::getFieldID(JNIEnv* env, const char* className, const char* name, const char* signature)
{
	jclass clazz = getClass(env, className);
	jfieldID id = env->GetFieldID(clazz, name, signature);
	assert(!env->ExceptionCheck());
	if (env->ExceptionCheck()) throw new JNIException(env);
	return id;
}


jfieldID JNIClass::getStaticFieldID(JNIEnv* env, jclass clazz, const char* name, const char* signature)
{
	jfieldID id = env->GetStaticFieldID(clazz, name, signature);
	assert(!env->ExceptionCheck());
	if (env->ExceptionCheck()) throw new JNIException(env);
	return id;
}


jmethodID JNIClass::getMethodID(JNIEnv* env, jobject object, const char* name, const char* signature)
{
	jmethodID id = env->GetMethodID(env->GetObjectClass(object), name, signature);
	assert(!env->ExceptionCheck());
	if (env->ExceptionCheck()) throw new JNIException(env);
	return id;
}

jmethodID JNIClass::getMethodID(JNIEnv* env, jclass clazz, const char* name, const char* signature)
{
	jmethodID id = env->GetMethodID(clazz, name, signature);
	assert(!env->ExceptionCheck());
	if (env->ExceptionCheck()) throw new JNIException(env);
	return id;
}

jmethodID JNIClass::getMethodID(JNIEnv* env, const char* className, const char* name, const char* signature)
{
	jclass clazz = getClass(env, className);
	jmethodID id = env->GetMethodID(clazz, name, signature);
	assert(!env->ExceptionCheck());
	if (env->ExceptionCheck()) throw new JNIException(env);
	return id;
}
