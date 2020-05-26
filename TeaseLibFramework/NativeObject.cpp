#include "stdafx.h"

#include <assert.h>

#include <jni.h>

#include "NativeObject.h"

#include "teaselib_core_jni_NativeObject.h"


extern "C"
{

    /*
    * Class:     teaselib_util_jni_NativeObject
    * Method:    disposeNativeObject
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_jni_NativeObject_dispose
    (JNIEnv *env, jobject jthis) {
        try {
            NativeObject* nativeObject = NativeObject::get(env, jthis);
            // TODO crashes - env = 0xCCCCCCCC -> uninitialized stack memory
            // assert(false);
            // delete nativeObject;
        }
        catch (NativeException& e) {
            JNIException::throwNew(env, e);
        }
        catch (JNIException& /*e*/) {
            // Forwarded automatically
        }
        catch (...) {
            // Ignore
        }
    }

}


JObject::JObject(JNIEnv* env) : env(env), jthis(nullptr) {
}

JObject::JObject(JNIEnv* env, jobject jthis) : env(env), jthis(env->NewGlobalRef(jthis)) {
}

JObject::~JObject() {
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

 NativeObject::NativeObject(JNIEnv* env) : JObject(env, nullptr) {
}

 NativeObject::NativeObject(JNIEnv* env, jobject jthis) : JObject(env, jthis) {
    jclass nativeObjectClass = env->GetObjectClass(jthis);
    if (env->ExceptionCheck()) throw JNIException(env);

    const jlong nativeObject = reinterpret_cast<jlong>(this);
    env->SetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"), nativeObject);
    if (env->ExceptionCheck()) throw JNIException(env);
}

 NativeObject::~NativeObject() {
}

 NativeObject* NativeObject::get(JNIEnv* env, jobject jthis) {
    if (jthis == NULL) return NULL;

    // Won't get the private field from the base class, but from the derived class
    jclass nativeObjectClass = env->GetObjectClass(jthis);
    if (env->ExceptionCheck()) throw JNIException(env);
    const jlong nativeObject = env->GetLongField(jthis, env->GetFieldID(nativeObjectClass, "nativeObject", "J"));
    if (env->ExceptionCheck()) throw JNIException(env);

    return reinterpret_cast<NativeObject*>(nativeObject);
}

void NativeObject::checkInitializedOrThrow(const NativeObject* nativeObject) {
    if (nativeObject == NULL) {
        assert(false);
        throw NativeException(E_POINTER, L"Unitialized native object");
    }
}
