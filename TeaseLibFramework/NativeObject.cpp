#include "stdafx.h"

#include <assert.h>

#include <jni.h>

#include "NativeObject.h"


JObject::JObject(JNIEnv* env) : env(env), jthis(nullptr)
{}

JObject::JObject(JNIEnv* env, jobject jthis)
    : env(env), jthis(env->NewGlobalRef(jthis))
{}

JObject::~JObject()
{
    if (jthis) {
        env->DeleteGlobalRef(jthis);
    }
}

JObject::operator jobject() const {
    return jthis;
}

JObject& JObject::operator=(jobject rvalue)
{
    if (jthis) {
        env->DeleteGlobalRef(jthis);
    }
    jthis = env->NewGlobalRef(rvalue);
    return *this;
}


NativeObject::NativeObject(JNIEnv* env)
    : JObject(env, nullptr)
{}

NativeObject::NativeObject(JNIEnv* env, jobject jthis)
    : JObject(env, jthis)
{
    jclass nativeObjectClass = env->GetObjectClass(jthis);
    if (env->ExceptionCheck()) throw JNIException(env);

    const jlong nativeObject = reinterpret_cast<jlong>(this);
    env->SetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"), nativeObject);
    if (env->ExceptionCheck()) throw JNIException(env);
}

NativeObject::~NativeObject()
{}


void NativeInstance::clear(JNIEnv* env, jobject jthis)
{
    Objects::requireNonNull(L"jenv", env);
    Objects::requireNonNull(L"jthis", jthis);

    jclass nativeObjectClass = env->GetObjectClass(jthis);
    if (env->ExceptionCheck()) throw JNIException(env);
    env->SetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"), 0);
    if (env->ExceptionCheck()) throw JNIException(env);
}
