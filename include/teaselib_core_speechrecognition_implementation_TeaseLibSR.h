/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class teaselib_core_speechrecognition_implementation_TeaseLibSR */

#ifndef _Included_teaselib_core_speechrecognition_implementation_TeaseLibSR
#define _Included_teaselib_core_speechrecognition_implementation_TeaseLibSR
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    initSR
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_initSR
  (JNIEnv *, jobject, jstring);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    initSREventThread
 * Signature: (Lteaselib/core/speechrecognition/SpeechRecognitionEvents;Ljava/util/concurrent/CountDownLatch;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_initSREventThread
  (JNIEnv *, jobject, jobject, jobject);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    setChoices
 * Signature: (Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_setChoices__Ljava_util_List_2
  (JNIEnv *, jobject, jobject);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    setChoices
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_setChoices___3B
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    setMaxAlternates
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_setMaxAlternates
  (JNIEnv *, jobject, jint);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    startRecognition
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_startRecognition
  (JNIEnv *, jobject);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    emulateRecognition
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_emulateRecognition
  (JNIEnv *, jobject, jstring);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    stopRecognition
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_stopRecognition
  (JNIEnv *, jobject);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_dispose
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
