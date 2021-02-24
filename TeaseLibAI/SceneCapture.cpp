#include "pch.h"

#include <JNIException.h>
#include <JNIString.h>
#include <JNIUtilities.h>

#include <NativeException.h>
#include <NativeObject.h>

#include <Video/AIfxVideoCapture.h>
#include <Pose/AIfxPoseEstimation.h>

#include <teaselib_core_ai_perception_SceneCapture.h>
#include "SceneCapture.h"

extern "C"
{

	/*
	 * Class:     teaselib_core_ai_perception_SceneCapture
	 * Method:    init
	 * Signature: (Ljava/lang/String;)J
	 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_perception_SceneCapture_newNativeInstance
	(JNIEnv* env, jclass, jstring jpath) {
		try {
			Objects::requireNonNull(L"path", jpath);
			aifx::VideoCapture* capture = new aifx::VideoCapture(JNIStringUTF8(env, jpath));
			return reinterpret_cast<jlong>(capture);
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
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
			aifx::VideoCapture* capture = NativeInstance::get<aifx::VideoCapture>(env, jthis);
			capture->start();
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
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
			aifx::VideoCapture* capture = NativeInstance::get<aifx::VideoCapture>(env, jthis);
			return capture->started();
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
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
			aifx::VideoCapture* capture = NativeInstance::get<aifx::VideoCapture>(env, jthis);
			capture->stop();
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
 * Class:     teaselib_core_ai_perception_SceneCapture
 * Method:    dispose
 * Signature: ()V
 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_SceneCapture_dispose
	(JNIEnv* env, jobject jthis)
	{
		try {
			aifx::VideoCapture* capture = NativeInstance::get<aifx::VideoCapture>(env, jthis);
			delete capture;
		}
		catch (std::invalid_argument& e) {
			JNIException::rethrow(env, e);
		}
		catch (std::exception& e) {
			JNIException::rethrow(env, e);
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
	}

}

const char* enclosureLocationEnumName[] = { "Front", "Rear", "External" };

SceneCapture::SceneCapture(JNIEnv* env, const aifx::VideoCapture::CameraInfo& cameraInfo)
	: NativeObject(env), device(new aifx::VideoCapture(cameraInfo.id))
{
	jclass clazz = JNIClass::getClass(env, "teaselib/core/ai/perception/SceneCapture");
	jthis = env->NewGlobalRef(env->NewObject(clazz,
		JNIClass::getMethodID(env, clazz,
			"<init>", 
			"(JLjava/lang/String;Lteaselib/core/ai/perception/SceneCapture$EnclosureLocation;)V"),
		reinterpret_cast<jlong>(device.get()),
		JNIString(env, cameraInfo.friendlyName.c_str()).operator jstring(),
		JNIUtilities::enumValue(env,
			"teaselib/core/ai/perception/SceneCapture$EnclosureLocation",
			enclosureLocationEnumName[(int)cameraInfo.enclosureLocation]))
	);
	if (env->ExceptionCheck()) throw JNIException(env);
}
