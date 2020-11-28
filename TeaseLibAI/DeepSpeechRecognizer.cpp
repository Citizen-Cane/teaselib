#include "pch.h"

#include <teaselib_core_ai_perception_DeepSpeechRecognizer.h>
#include "DeepSpeechRecognizer.h"

extern "C"
{

	/*
	* Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	* Method:    init
	* Signature: (Ljava/lang/String;)J
	*/
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_perception_DeepSpeechRecognizer_init
	(JNIEnv* env, jclass, jstring jlanguageCode)
	{
		return 0;
	}

	/*
		* Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
		* Method:    process
		* Signature: (Lteaselib/core/speechrecognition/SpeechRecognitionEvents;Ljava/util/concurrent/CountDownLatch;)V
		*/
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_DeepSpeechRecognizer_process
	(JNIEnv* env, jobject, jobject jthis, jobject jevents)
	{
	}

	/*
 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
 * Method:    languageCode
 * Signature: ()Ljava/lang/String;
 */
	JNIEXPORT jstring JNICALL Java_teaselib_core_ai_perception_DeepSpeechRecognizer_languageCode
	(JNIEnv* env, jobject  jthis)
	{
		return nullptr;
	}

	/*
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    startRecognition
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_DeepSpeechRecognizer_startRecognition
	(JNIEnv* env, jobject jthis)
	{
	}

	/*
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    emulateRecognition
	 * Signature: (Ljava/lang/String;)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_DeepSpeechRecognizer_emulateRecognition
	(JNIEnv* env, jobject jthis, jstring jspeech)
	{
	}

	/*
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    stopRecognition
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_DeepSpeechRecognizer_stopRecognition
	(JNIEnv* env, jobject jthis)
	{
	}


	/*
		* Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
		* Method:    dispose
		* Signature: ()V
		*/
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_DeepSpeechRecognizer_dispose
	(JNIEnv* env, jobject jthis)
	{
	}

}
