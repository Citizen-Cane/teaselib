#include "pch.h"

#include<JNIUtilities.h>
#include<NativeObject.h>

#include <AIfxOpenCL.h>

#include <teaselib_core_ai_TeaseLibAI.h>


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
		std::vector<NativeObject*> sceneCaptures;
		return env->NewGlobalRef(JNIUtilities::asList(env, sceneCaptures));
	}

}
