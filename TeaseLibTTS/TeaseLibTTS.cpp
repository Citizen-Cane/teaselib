// TeaseLibTTS.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"

#include <algorithm>
#include <vector>

#include <assert.h>

#include <atlbase.h>
#include <sapi.h>

#include <sphelper.h>

#include <COMException.h>
#include <COMUser.h>
#include <JNIException.h>
#include <JNIString.h>
#include <JNIUtilities.h>
#include <NativeException.h>
#include <NativeObject.h>

#include <teaselib_core_texttospeech_implementation_TeaseLibTTS.h>

#include "SpeechSynthesizer.h"
#include "Voice.h"

extern "C"
{
	/*
 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
 * Method:    newNativeInstance
 * Signature: ()J
 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_newNativeInstance
	(JNIEnv* env, jclass)
	{
		try {
			SpeechSynthesizer* speechSynthesizer = new SpeechSynthesizer;
			return reinterpret_cast<jlong>(speechSynthesizer);
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return 0;
		}
		catch (JNIException& e) {
			e.rethrow();
			return 0;
		}
	}

	/*
	* Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
	* Method:	 addLexiconEntry
	* Signature: (Ljava / lang / String; Ljava / lang / String; Ljava / lang / String;)V
	*/

	JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_addLexiconEntry
		(JNIEnv *env, jobject jthis, jstring locale, jstring word,  jint partOfSpeech, jstring pronunciation)
	{
		try {
			Objects::requireNonNull(L"locale", locale);
			Objects::requireNonNull(L"pronunciation", pronunciation);
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
			speechSynthesizer->addLexiconEntry(JNIString(env, locale), JNIString(env, word), static_cast<SPPARTOFSPEECH>(partOfSpeech), JNIString(env, pronunciation));
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	* Class:     teaselib_texttospeech_implementation_TeaseLibTTS
	* Method:    getInstalledVoices
	* Signature: (Ljava/util/Map;)V
	*/
	JNIEXPORT jobject JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_getVoices
		(JNIEnv *env, jobject jthis)
	{
		try {
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
			return speechSynthesizer->voices(env, jthis);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
		return nullptr;
	}

    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    setVoice
    * Signature: (Lteaselib/texttospeech/Voice;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_setVoice
    (JNIEnv *env, jobject jthis, jobject jvoice) 
	{
        try {
			Objects::requireNonNull(L"voice", jvoice);
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
			const Voice* voice = NativeInstance::get<Voice>(env, jvoice);
            // A null voice is a valid value, it denotes the system default voice
			speechSynthesizer->setVoice(voice);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
    }

	extern "C++" {
		jobjectArray getHints(JNIEnv* env, jobject jthis)
	{
			jclass jclass = env->GetObjectClass(jthis);
			jobjectArray hints = (jobjectArray)env->GetObjectField(jthis, env->GetFieldID(jclass, "hints", "[Ljava/lang/String;"));
			if (env->ExceptionCheck()) {
				throw JNIException(env);
			}
			return hints;
		}
	}

    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    speak
    * Signature: (Ljava/lang/String;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_speak__Ljava_lang_String_2
    (JNIEnv *env, jobject jthis, jstring prompt)
	{
        try {
			Objects::requireNonNull(L"prompt", prompt);
			
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
            speechSynthesizer->applyHints(JNIUtilities::wstringArray(env, getHints(env, jthis)));
            speechSynthesizer->speak(JNIString(env, prompt));
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
    }

    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    speak
    * Signature: (Ljava/lang/String;Ljava/lang/String;)V
    */
    JNIEXPORT jstring JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_speak__Ljava_lang_String_2Ljava_lang_String_2
    (JNIEnv *env, jobject jthis, jstring prompt, jstring path) 
	{
        jstring actualPath = NULL;
        try {
			Objects::requireNonNull(L"prompt", prompt);
			Objects::requireNonNull(L"path", path);
			
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
            speechSynthesizer->applyHints(JNIUtilities::wstringArray(env, getHints(env, jthis)));
            const std::wstring soundFile = speechSynthesizer->speak(JNIString(env, prompt), JNIString(env, path));
            actualPath = JNIString(env, soundFile.c_str()).detach();
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
        return actualPath;
    }


    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    stop
    * Signature: ()V
    */
	JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_stop
	(JNIEnv* env, jobject jthis)
	{
		try {
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
			speechSynthesizer->stop();
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
	}

    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    dispose
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_dispose
    (JNIEnv *env, jobject jthis)
	{
        SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
        delete speechSynthesizer;
    }

}
