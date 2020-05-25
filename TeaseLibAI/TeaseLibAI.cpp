#include "pch.h"

#include<JNIUtilities.h>
#include<NativeObject.h>

#include <teaselib_core_ai_TeaseLibAI.h>


/*
 * Class:     teaselib_core_ai_TeaseLibAI
 * Method:    sceneCaptures
 * Signature: ()Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_teaselib_core_ai_TeaseLibAI_sceneCaptures
(JNIEnv*env, jobject teaseLibAI) {
	std::vector<NativeObject*> sceneCaptures;
	return env->NewGlobalRef(JNIUtilities::jList(env, sceneCaptures));
}
