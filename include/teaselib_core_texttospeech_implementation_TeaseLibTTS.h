/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class teaselib_core_texttospeech_implementation_TeaseLibTTS */

#ifndef _Included_teaselib_core_texttospeech_implementation_TeaseLibTTS
#define _Included_teaselib_core_texttospeech_implementation_TeaseLibTTS
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
 * Method:    getInstalledVoices
 * Signature: (Ljava/util/Map;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_getInstalledVoices
  (JNIEnv *, jclass, jobject);

/*
 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
 * Method:    setVoice
 * Signature: (Lteaselib/core/texttospeech/Voice;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_setVoice
  (JNIEnv *, jobject, jobject);

/*
 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
 * Method:    speak
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_speak__Ljava_lang_String_2
  (JNIEnv *, jobject, jstring);

/*
 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
 * Method:    speak
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_speak__Ljava_lang_String_2Ljava_lang_String_2
  (JNIEnv *, jobject, jstring, jstring);

/*
 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
 * Method:    stop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_stop
  (JNIEnv *, jobject);

/*
 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_dispose
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
