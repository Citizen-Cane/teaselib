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
 * Signature: (Lteaselib/core/speechrecognition/SpeechRecognitionEvents;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_initSR
  (JNIEnv *, jobject, jobject, jstring);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    initSREventThread
 * Signature: (Ljava/lang/Thread;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_initSREventThread
  (JNIEnv *, jobject, jobject);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    setChoices
 * Signature: (Ljava/util/List;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_setChoices
  (JNIEnv *, jobject, jobject);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    setMaxAlternates
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_setMaxAlternates
  (JNIEnv *, jobject, jint);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    startRecognition
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_startRecognition
  (JNIEnv *, jobject);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    emulateRecognition
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_emulateRecognition
  (JNIEnv *, jobject, jstring);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    stopRecognition
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_stopRecognition
  (JNIEnv *, jobject);

/*
 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_dispose
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
