// TeaseLibTTS.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"

#include <algorithm>
#include <map>
#include <memory>
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
#include <teaselib_core_texttospeech_NativeVoice.h>

#include "SpeechSynthesizer.h"
#include "Voice.h"

#include "SpSpeechSynthesizer.h"
#include "LoquendoSpeechSynthesizer.h"

using namespace std;

extern "C"
{
	/*
	 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
	 * Method:    newNativeInstance
	 * Signature: (Ljava/lang/String;)J
	 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_newNativeInstance
	(JNIEnv* env, jclass, jstring jsdk)
	{
		try {
			const JNIStringUTF8 sdk(env, jsdk);
			SpeechSynthesizer* speechSynthesizer;
			if (sdk == LoquendoSpeechSynthesizer::Sdk) {
				try {
					speechSynthesizer = new LoquendoSpeechSynthesizer(env);
				} catch (invalid_argument& e) {
					JNIException::rethrow(env, e, "java/lang/UnsupportedOperationException");
					return 0;
				}
			} else if (sdk == SpSpeechSynthesizer::Sdk) {
				speechSynthesizer = new SpSpeechSynthesizer(env);
			} else {
				throw invalid_argument(sdk);
			}
			return reinterpret_cast<jlong>(speechSynthesizer);
		}
		catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
			return 0;
		} catch (exception& e) {
			JNIException::rethrow(env, e);
			return 0;
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return 0;
		} catch (JNIException& e) {
			e.rethrow();
			return 0;
		}
	}

	/*
	 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
	 * Method:    addLexiconEntry
	 * Signature: (Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_addLexiconEntry
	(JNIEnv *env, jobject jthis, jstring locale, jstring word,  jint partOfSpeech, jstring pronunciation)
	{
		try {
			Objects::requireNonNull(L"locale", locale);
			Objects::requireNonNull(L"pronunciation", pronunciation);
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
			speechSynthesizer->addLexiconEntry(JNIString(env, locale), JNIString(env, word), static_cast<SPPARTOFSPEECH>(partOfSpeech), JNIString(env, pronunciation));
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
	 * Method:    getVoices
	 * Signature: ()Ljava/util/List;
	 */
	JNIEXPORT jobject JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_getVoices
	(JNIEnv *env, jobject jthis)
	{
		try {
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
			jobject jvoices = speechSynthesizer->voices(jthis);
			if (jvoices == nullptr) {
				jvoices = JNIUtilities::emptyList(env);
			}
			return jvoices;
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
		return nullptr;
	}

	/*
	 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
	 * Method:    setVoice
	 * Signature: (Lteaselib/core/texttospeech/Voice;)V
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
		} catch (exception& e) {
			JNIException::rethrow(env, e);
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
	 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
	 * Method:    speak
	 * Signature: (Ljava/lang/String;)V
	 */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_speak__Ljava_lang_String_2
    (JNIEnv *env, jobject jthis, jstring prompt)
	{
        try {
			Objects::requireNonNull(L"prompt", prompt);
			
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
            speechSynthesizer->setHints(JNIUtilities::wstringArray(env, getHints(env, jthis)));
            speechSynthesizer->speak(JNIString(env, prompt));
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
    }

	/*
	 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
	 * Method:    speak
	 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
	 */
    JNIEXPORT jstring JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_speak__Ljava_lang_String_2Ljava_lang_String_2
    (JNIEnv *env, jobject jthis, jstring prompt, jstring path) 
	{
        jstring actualPath = NULL;
        try {
			Objects::requireNonNull(L"prompt", prompt);
			Objects::requireNonNull(L"path", path);
			
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
            speechSynthesizer->setHints(JNIUtilities::wstringArray(env, getHints(env, jthis)));
            const std::wstring soundFile = speechSynthesizer->speak(JNIString(env, prompt), JNIString(env, path));
            actualPath = JNIString(env, soundFile.c_str()).detach();
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
        return actualPath;
    }


	/*
	 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
	 * Method:    stop
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_stop
	(JNIEnv* env, jobject jthis)
	{
		try {
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
			speechSynthesizer->stop();
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_texttospeech_implementation_TeaseLibTTS
	 * Method:    dispose
	 * Signature: ()V
	 */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_dispose
    (JNIEnv *env, jobject jthis)
	{
		try {
			SpeechSynthesizer* speechSynthesizer = NativeInstance::get<SpeechSynthesizer>(env, jthis);
        delete speechSynthesizer;
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}


	////////


	/*
	 * Class:     teaselib_core_texttospeech_NativeVoice
	 * Method:    dispose
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_NativeVoice_dispose
	(JNIEnv* env, jobject jthis)
	{
		try {
			Voice* voice = NativeInstance::get<Voice>(env, jthis);
			delete voice;
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

}
