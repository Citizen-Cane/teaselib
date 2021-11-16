#include "pch.h"

#include <algorithm>
#include <vector>

#include <JNIException.h>
#include <JNIString.h>
#include <JNIUtilities.h>

#include <NativeException.h>
#include <NativeObject.h>

#include <Video/VideoCapture.h>

#include <teaselib_core_ai_perception_SceneCapture.h>
#include "SceneCapture.h"

using namespace aifx::video;
using namespace std;

extern "C"
{

	/*
	 * Class:     teaselib_core_ai_perception_SceneCapture
	 * Method:    devices
	 * Signature: ()Lteaselib/core/jni/NativeObjectList;
	 */
	JNIEXPORT jobject JNICALL Java_teaselib_core_ai_perception_SceneCapture_devices
	(JNIEnv* env, jclass)
	{
		try {
			auto cameras = VideoCapture::devices();
			vector<NativeObject*> sceneCaptures;

			for_each(cameras.begin(), cameras.end(), [&sceneCaptures, env](const VideoCapture::Devices::value_type& cameraInfo) {
				sceneCaptures.push_back(new SceneCapture(env, cameraInfo.second));
			});

			return JNIUtilities::asList(env, sceneCaptures);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
			return nullptr;
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return nullptr;
		} catch (JNIException& e) {
			e.rethrow();
			return nullptr;
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_SceneCapture
	 * Method:    init
	 * Signature: (Ljava/lang/String;)J
	 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_perception_SceneCapture_newNativeInstance
	(JNIEnv* env, jclass, jstring jpath) {
		try {
			Objects::requireNonNull(L"path", jpath);
			VideoCapture* capture = new VideoCapture(JNIStringUTF8(env, jpath));
			return reinterpret_cast<jlong>(capture);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
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
			VideoCapture* capture = NativeInstance::get<VideoCapture>(env, jthis);
			capture->start();
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
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
			VideoCapture* capture = NativeInstance::get<VideoCapture>(env, jthis);
			return capture->started();
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
		return JNI_FALSE;
	}

	/*
	 * Class:     teaselib_core_ai_perception_SceneCapture
	 * Method:    stop
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_SceneCapture_stop
	(JNIEnv* env, jobject jthis) {
		try {
			VideoCapture* capture = NativeInstance::get<VideoCapture>(env, jthis);
			capture->stop();
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
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
			VideoCapture* capture = NativeInstance::get<VideoCapture>(env, jthis);
			delete capture;
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

}


const char* enclosureLocationEnumName[] = { "Front", "Rear", "External" };

SceneCapture::SceneCapture(JNIEnv* env, const VideoCapture::CameraInfo& cameraInfo)
	: NativeObject(env), device(new VideoCapture(cameraInfo.id))
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
