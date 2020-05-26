#pragma once

#include <jni.h>

#include "NativeException.h"
#include "JNIException.h"

class JObject {
public:
	JObject(JNIEnv* env);

	JObject(JNIEnv* env, jobject jthis);

	virtual ~JObject();

	operator jobject() const;

	JObject& operator=(jobject rvalue);

protected:
	JNIEnv* env;
	jobject jthis;
};

class NativeObject : public JObject {
public:
	NativeObject(JNIEnv* env);
	NativeObject(JNIEnv* env, jobject jthis);
	virtual ~NativeObject();

	static NativeObject* get(JNIEnv* env, jobject jthis);
	static void checkInitializedOrThrow(const NativeObject* nativeObject);
private:
};
