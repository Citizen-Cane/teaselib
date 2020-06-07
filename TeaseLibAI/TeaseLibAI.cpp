#include "pch.h"

#include <vector>

#include<JNIUtilities.h>
#include<NativeObject.h>

#include <AIfxOpenCL.h>
#include <AIfxVideoCapture.h>

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
		// TODO add front camera
		// TODO get names of opencv devices -> copy from videoInput lib
		// TODO enum capture devices
		auto cameras = aifx::VideoCapture::devices();
		std::vector<NativeObject*> sceneCaptures;

		std::for_each(cameras.begin(), cameras.end(), [&sceneCaptures, env](const aifx::VideoCapture::Devices::value_type& cameraInfo) {
			sceneCaptures.push_back(new SceneCapture(env, cameraInfo.first, cameraInfo.second.c_str()));
		});

		return env->NewGlobalRef(JNIUtilities::asList(env, sceneCaptures));
	}

}
