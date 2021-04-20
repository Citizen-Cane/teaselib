#include "pch.h"

#include <algorithm>
#include <vector>

#include<JNIUtilities.h>
#include<NativeObject.h>

#include <Compute/OpenCL.h>
#include <Video/VideoCapture.h>

#include <teaselib_core_ai_TeaseLibAI.h>

#include "SceneCapture.h"

using namespace aifx::compute;
using namespace aifx::video;
using namespace std;

extern "C"
{

	/*
 * Class:     teaselib_core_ai_TeaseLibAI
 * Method:    initOpenCL
 * Signature: ()Z
 */
	JNIEXPORT jboolean JNICALL Java_teaselib_core_ai_TeaseLibAI_initOpenCL
	(JNIEnv* env, jobject) {
		try {
			return OpenCL::init();
		} catch (exception& e) {
			JNIException::rethrow(env, e);
			return JNI_FALSE;
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return JNI_FALSE;
		} catch (JNIException& e) {
			e.rethrow();
			return JNI_FALSE;
		}
	}

	/*
	 * Class:     teaselib_core_ai_TeaseLibAI
	 * Method:    sceneCaptures
	 * Signature: ()Ljava/util/List;
	 */
	JNIEXPORT jobject JNICALL Java_teaselib_core_ai_TeaseLibAI_sceneCaptures
	(JNIEnv* env, jobject) {
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

}
