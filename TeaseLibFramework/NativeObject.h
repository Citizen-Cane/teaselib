#pragma once

#include <stdexcept>

#include <jni.h>

#include "NativeException.h"
#include "JNIException.h"

class TEASELIB_FRAMEWORK_EXPORT JObject {
public:
	JObject(JNIEnv* env);
	JObject(JNIEnv* env, jobject jthis);
	virtual ~JObject();

	operator jobject() const;
	JObject& operator=(jobject rvalue);

protected:
	JNIEnv* const env;
	jobject jthis;
};

class TEASELIB_FRAMEWORK_EXPORT NativeObject : public JObject {
public:
	NativeObject(JNIEnv* env);
	/* _declspec(deprecated("Use JNI-supplied jthis instead of member - remove jthis from native object class")) */
	NativeObject(JNIEnv* env, jobject jthis);
	virtual ~NativeObject();


};

class TEASELIB_FRAMEWORK_EXPORT NativeInstance {
public:
	template<class T> static T* get(JNIEnv* env, jobject jthis)
	{
		Objects::requireNonNull(L"jenv", env);
		Objects::requireNonNull(L"jthis", jthis);

		jclass nativeObjectClass = env->GetObjectClass(jthis);
		if (env->ExceptionCheck()) throw JNIException(env);
		const jlong nativeObject = env->GetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"));
		if (env->ExceptionCheck()) throw JNIException(env);

		if (nativeObject == 0) {
			throw NativeException(E_POINTER, L"Unitialized native object");
		}

		return reinterpret_cast<T*>(nativeObject);
	}

	static void clear(JNIEnv* env, jobject jthis);

	template<class T> static void dispose(JNIEnv* env, jobject jthis, T* nativeObject)
	{
		Objects::requireNonNull(L"Native object", nativeObject);
		const T* existing = get<T>(env, jthis);
		if (nativeObject == existing) {
			delete nativeObject;
		} else {
			throw std::logic_error("native object already disposed");
		}
	}
};