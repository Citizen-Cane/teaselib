#include "pch.h"

#include <vector>

#include<JNIUtilities.h>
#include<NativeObject.h>

#include <Compute/AIfxOpenCL.h>
#include <Video/AIfxVideoCapture.h>

#include <teaselib_core_ai_TeaseLibAI.h>

#include "SceneCapture.h"

extern "C"
{

	/*
 * Class:     teaselib_core_ai_TeaseLibAI
 * Method:    initOpenCL
 * Signature: ()Z
 */
	JNIEXPORT jboolean JNICALL Java_teaselib_core_ai_TeaseLibAI_initOpenCL
	(JNIEnv*, jobject) {
		return aifx::OpenCL::init();
	}

	/*
	 * Class:     teaselib_core_ai_TeaseLibAI
	 * Method:    sceneCaptures
	 * Signature: ()Ljava/util/List;
	 */
	JNIEXPORT jobject JNICALL Java_teaselib_core_ai_TeaseLibAI_sceneCaptures
	(JNIEnv* env, jobject) {
		auto cameras = aifx::VideoCapture::devices();
		std::vector<NativeObject*> sceneCaptures;

		std::for_each(cameras.begin(), cameras.end(), [&sceneCaptures, env](const aifx::VideoCapture::Devices::value_type& cameraInfo) {
			sceneCaptures.push_back(new SceneCapture(env, cameraInfo.second));
		});

		return env->NewGlobalRef(JNIUtilities::asList(env, sceneCaptures));
	}

}
