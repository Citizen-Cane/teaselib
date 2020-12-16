/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class teaselib_core_ai_perception_HumanPose */

#ifndef _Included_teaselib_core_ai_perception_HumanPose
#define _Included_teaselib_core_ai_perception_HumanPose
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     teaselib_core_ai_perception_HumanPose
 * Method:    newNativeInstance
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_teaselib_core_ai_perception_HumanPose_newNativeInstance
  (JNIEnv *, jclass);

/*
 * Class:     teaselib_core_ai_perception_HumanPose
 * Method:    setInterests
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_HumanPose_setInterests
  (JNIEnv *, jobject, jint);

/*
 * Class:     teaselib_core_ai_perception_HumanPose
 * Method:    acquire
 * Signature: (Lteaselib/core/ai/perception/SceneCapture;Lteaselib/core/ai/perception/SceneCapture/Rotation;)Z
 */
JNIEXPORT jboolean JNICALL Java_teaselib_core_ai_perception_HumanPose_acquire
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     teaselib_core_ai_perception_HumanPose
 * Method:    acquireImage
 * Signature: ([B)Z
 */
JNIEXPORT jboolean JNICALL Java_teaselib_core_ai_perception_HumanPose_acquireImage
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     teaselib_core_ai_perception_HumanPose
 * Method:    estimate
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_HumanPose_estimate
  (JNIEnv *, jobject);

/*
 * Class:     teaselib_core_ai_perception_HumanPose
 * Method:    results
 * Signature: ()Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_teaselib_core_ai_perception_HumanPose_results
  (JNIEnv *, jobject);

/*
 * Class:     teaselib_core_ai_perception_HumanPose
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_HumanPose_dispose
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
