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
	(JNIEnv*, jobject) {
		return OpenCL::init();
	}

	/*
	 * Class:     teaselib_core_ai_TeaseLibAI
	 * Method:    sceneCaptures
	 * Signature: ()Ljava/util/List;
	 */
	JNIEXPORT jobject JNICALL Java_teaselib_core_ai_TeaseLibAI_sceneCaptures
	(JNIEnv* env, jobject) {
		auto cameras = VideoCapture::devices();
		vector<NativeObject*> sceneCaptures;

		for_each(cameras.begin(), cameras.end(), [&sceneCaptures, env](const VideoCapture::Devices::value_type& cameraInfo) {
			sceneCaptures.push_back(new SceneCapture(env, cameraInfo.second));
		});

		return env->NewGlobalRef(JNIUtilities::asList(env, sceneCaptures));
	}

}
