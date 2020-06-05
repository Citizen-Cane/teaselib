#include "pch.h"

#include <JNIException.h>
#include <JNIString.h>
#include <NativeException.h>
#include <NativeObject.h>

#include <AIfxVideoCapture.h>
#include <AIfxPoseEstimation.h>

#include <teaselib_core_ai_perception_SceneCapture.h>
#include "SceneCapture.h"

extern "C"
{

	/*
	 * Class:     teaselib_core_ai_perception_SceneCapture
	 * Method:    init
	 * Signature: (Ljava/lang/String;)J
	 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_perception_SceneCapture_init
	(JNIEnv* env, jclass, jstring jpath) {
		try {
			Objects::requireNonNull(L"path", jpath);
			aifx::VideoCapture* capture = new aifx::VideoCapture(JNIStringUTF8(env, jpath));
			return reinterpret_cast<jlong>(capture);
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
		return 0;
	}

	/*
	 * Class:     teaselib_core_ai_perception_SceneCapture
	 * Method:    start
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_SceneCapture_start
	(JNIEnv* env, jobject jthis) {
		try {
			aifx::VideoCapture* capture = reinterpret_cast<aifx::VideoCapture*>(NativeObject::get(env, jthis));
			capture->start();
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_SceneCapture
	 * Method:    isStarted
	 * Signature: ()Z
	 */
	JNIEXPORT jboolean JNICALL Java_teaselib_core_ai_perception_SceneCapture_isStarted
	(JNIEnv* env, jobject jthis) {
		try {
			aifx::VideoCapture* capture = reinterpret_cast<aifx::VideoCapture*>(NativeObject::get(env, jthis));
			return capture->started();
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
		return 0;
	}

	/*
	 * Class:     teaselib_core_ai_perception_SceneCapture
	 * Method:    stop
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_SceneCapture_stop
	(JNIEnv* env, jobject jthis) {
		try {
			aifx::VideoCapture* capture = reinterpret_cast<aifx::VideoCapture*>(NativeObject::get(env, jthis));
			capture->stop();
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
	}

}

SceneCapture::SceneCapture(JNIEnv* env, int id, const wchar_t* name)
	: NativeObject(env), device(new aifx::VideoCapture(id))
{
	jclass clazz = env->FindClass("teaselib/core/ai/perception/SceneCapture");
	if (env->ExceptionCheck()) throw JNIException(env);

	const char* signature = "(JLjava/lang/String;)V";
	jthis = env->NewGlobalRef(env->NewObject(clazz,
		JNIClass::getMethodID(env, clazz, "<init>", signature),
		reinterpret_cast<jlong>(device.get()),
		JNIString(env, name).operator jstring()));
	if (env->ExceptionCheck()) throw JNIException(env);
}
