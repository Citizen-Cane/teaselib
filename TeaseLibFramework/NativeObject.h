#pragma once

#include <jni.h>

#include "NativeException.h"
#include "JNIException.h"

class JObject {
public:
	JObject(JNIEnv* env) : env(env), jthis(nullptr) {
	}

	JObject(JNIEnv* env, jobject jthis) : env(env), jthis(env->NewGlobalRef(jthis)) {
	}

	virtual ~JObject() {
		if (jthis) {
			env->DeleteGlobalRef(jthis);
		}
	}

	operator jobject() const {
		return jthis;
	}

	JObject &operator=(jobject rvalue) {
		if (jthis) {
			env->DeleteGlobalRef(jthis);
		}
		jthis = env->NewGlobalRef(rvalue);
		return *this;
	}

protected:
	JNIEnv* env;
	jobject jthis;
};

class NativeObject : public JObject {
public:
	NativeObject(JNIEnv *env) : JObject(env, nullptr) {
	}

	NativeObject(JNIEnv *env, jobject jthis) : JObject(env, jthis) {
		jclass nativeObjectClass = env->GetObjectClass(jthis);
		if (env->ExceptionCheck()) throw JNIException(env);

		const jlong nativeObject = reinterpret_cast<jlong>(this);
		env->SetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"), nativeObject);
		if (env->ExceptionCheck()) throw JNIException(env);
	}

	virtual ~NativeObject() {
	}

	static NativeObject* get(JNIEnv * env, jobject jthis) {
		if (jthis == NULL) return NULL;

		// Won't get the private field from the base class, but from the derived class
		jclass nativeObjectClass = env->GetObjectClass(jthis);
		if (env->ExceptionCheck()) throw JNIException(env);
		const jlong nativeObject = env->GetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"));
		if (env->ExceptionCheck()) throw JNIException(env);

		return reinterpret_cast<NativeObject*>(nativeObject);
	}

	static void dispose(JNIEnv * env, jobject jthis) {
		try {
			NativeObject* nativeObject = get(env, jthis);
			delete nativeObject;
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException& /*e*/)	{
			// Forwarded automatically
		}
		catch (...)	{
			// Ignore
		}
	}

	static void checkInitializedOrThrow(const NativeObject* nativeObject) {
		if (nativeObject == NULL) {
			assert(false);
			throw NativeException(E_POINTER, L"Unitialized native object");
		}
	}
};
