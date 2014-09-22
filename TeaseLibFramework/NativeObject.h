#pragma once

#include <jni.h>

#include <JNIException.h>

class NativeObject
{
public:
	NativeObject(JNIEnv *env)
		: env(env)
	{
	}

	NativeObject(JNIEnv *env, jobject jthis)
		: env(env)
	{
		jclass nativeObjectClass = env->GetObjectClass(jthis);
		if (env->ExceptionCheck()) throw new JNIException(env);
		const jlong nativeObject = reinterpret_cast<jlong>(this);
		env->SetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"), nativeObject);
		if (env->ExceptionCheck()) throw new JNIException(env);
	}

	virtual ~NativeObject()
	{
	}

	operator jobject() const
	{
		return jthis;
	}

	NativeObject &operator=(jobject rvalue)
	{
		this->jthis = rvalue;
		return *this;
	}

	static NativeObject* get(JNIEnv * env, jobject jthis)
	{
		if (jthis == NULL) return NULL;
		// Won't get the private field from the base class, but from the derived class
		jclass nativeObjectClass = env->GetObjectClass(jthis);
		if (env->ExceptionCheck()) throw new JNIException(env);
		jlong x = env->GetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"));
		if (env->ExceptionCheck()) throw new JNIException(env);
		NativeObject* nativeObject = reinterpret_cast<NativeObject*>(x);
		*nativeObject = jthis;
		return nativeObject;
	}

	static void dispose(JNIEnv * env, jobject jthis)
	{
		try
		{
			NativeObject* nativeObject = get(env, jthis);
			delete nativeObject;
		}
		catch (NativeException* e)
		{
			JNIException::throwNew(env, e);
		}
		catch (JNIException* /*e*/)
		{
			// Forwarded automatically
		}
		catch (...)
		{
			// Ignore
		}
	}
protected:
	jobject jthis;
	JNIEnv *env;
};
