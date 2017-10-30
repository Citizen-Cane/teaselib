#pragma once

#include <jni.h>

#include <NativeException.h>
#include <JNIException.h>

class NativeObject {
public:
	NativeObject(JNIEnv *env) : env(env) {
	}

	NativeObject(JNIEnv *env, jobject jthis) : env(env), jthis(env->NewGlobalRef(jthis)) {
		jclass nativeObjectClass = env->GetObjectClass(jthis);
		if (env->ExceptionCheck()) throw new JNIException(env);

		const jlong nativeObject = reinterpret_cast<jlong>(this);
		env->SetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"), nativeObject);
		if (env->ExceptionCheck()) throw new JNIException(env);
	}

	virtual ~NativeObject()	{
		if (jthis) {
			env->DeleteGlobalRef(this->jthis);
		}
	}

	operator jobject() const {
		return jthis;
	}

	NativeObject &operator=(jobject rvalue) {
		if (jthis) {
			env->DeleteGlobalRef(this->jthis);
		}
		this->jthis = env->NewGlobalRef(rvalue);
		return *this;
	}

	static NativeObject* get(JNIEnv * env, jobject jthis) {
		if (jthis == NULL) return NULL;

		// Won't get the private field from the base class, but from the derived class
		jclass nativeObjectClass = env->GetObjectClass(jthis);
		if (env->ExceptionCheck()) throw new JNIException(env);
		const jlong nativeObject = env->GetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"));
		if (env->ExceptionCheck()) throw new JNIException(env);

		return reinterpret_cast<NativeObject*>(nativeObject);
	}

	static void dispose(JNIEnv * env, jobject jthis) {
		try {
			NativeObject* nativeObject = get(env, jthis);
			delete nativeObject;
		}
		catch (NativeException* e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException* /*e*/)	{
			// Forwarded automatically
		}
		catch (...)	{
			// Ignore
		}
	}

	static void checkInitializedOrThrow(const NativeObject* nativeObject) {
		if (nativeObject == NULL) {
			assert(false);
			throw new NativeException(E_POINTER, L"Unitialized native object");
		}
	}
protected:
	jobject jthis;
	JNIEnv *env;
};
