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
	static const char* enclosureLocationEnumName[] = { "Front", "Rear", "External" };

	/*
	 * Class:     teaselib_core_ai_perception_SceneCapture
	 * Method:    devices
	 * Signature: ()Lteaselib/core/jni/NativeObjectList;
	 */
	JNIEXPORT jobject JNICALL Java_teaselib_core_ai_perception_SceneCapture_devices
	(JNIEnv* env, jclass)
	{
		try {
			vector<jobject> sceneCaptures;
			jclass clazz = JNIClass::getClass(env, "teaselib/core/ai/perception/SceneCapture");
			jmethodID constructor = JNIClass::getMethodID(env, clazz,
				"<init>",
				"(JLjava/lang/String;Lteaselib/core/ai/perception/SceneCapture$EnclosureLocation;)V");
			for(const auto& [name,info] : VideoCapture::devices()) {
				SceneCapture* device = new SceneCapture(new VideoCapture(info.id));
				jobject jscenecapture = env->NewGlobalRef(env->NewObject(clazz, constructor,
					reinterpret_cast<jlong>(device),
					JNIString(env, info.friendlyName.c_str()).operator jstring(),
					JNIUtilities::enumValue(env,
						"teaselib/core/ai/perception/SceneCapture$EnclosureLocation",
						enclosureLocationEnumName[(int) info.enclosureLocation]))
				);
				if (env->ExceptionCheck()) throw JNIException(env);
				sceneCaptures.push_back(jscenecapture);
			}
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
			SceneCapture* device = new SceneCapture(new VideoCapture(JNIStringUTF8(env, jpath)));
			return reinterpret_cast<jlong>(device);
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
			VideoCapture* device = SceneCapture::nativeInstance(env, jthis)->device;
			device->start();
			if (device->source() == VideoCapture::Source::Camera) {
				device->size(VideoCapture::Quality::VGA, VideoCapture::Aspect::Sensor);
			}
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
			VideoCapture* device = SceneCapture::nativeInstance(env, jthis)->device;
			return device->started();
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
			VideoCapture* device = SceneCapture::nativeInstance(env, jthis)->device;
			device->stop();
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
			SceneCapture* device = SceneCapture::nativeInstance(env, jthis);
			delete device;
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

SceneCapture* SceneCapture::nativeInstance(JNIEnv* env, jobject jthis)
{
	return NativeInstance::get<SceneCapture>(env, jthis);
}

SceneCapture::SceneCapture(aifx::video::VideoCapture* device)
	: device(device)
{}

SceneCapture::~SceneCapture()
{
	if (device) {
		device->stop();
		delete device;
	}
}
