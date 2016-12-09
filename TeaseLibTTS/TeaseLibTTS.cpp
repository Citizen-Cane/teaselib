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
	const wchar_t* SPCAT_VOICES_ONECORE = L"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Speech_OneCore\\Voices";
	const wchar_t* SPCAT_VOICES_SPEECH_SERVER = L"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Speech Server\\v11.0\\Voices";

	const wchar_t* voiceCategories[] = { SPCAT_VOICES_ONECORE, SPCAT_VOICES_SPEECH_SERVER,  SPCAT_VOICES };

	HRESULT addVoices(const wchar_t* pszCatName, std::vector<Voice*>& voices, JNIEnv *env);
	void buildVoiceMap(const std::vector<Voice*>& voices, JNIEnv *env, jobject voiceMap);

	/*
	* Class:     teaselib_texttospeech_implementation_TeaseLibTTS
	* Method:    getInstalledVoices
	* Signature: (Ljava/util/Map;)V
	*/
	JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_getInstalledVoices
		(JNIEnv *env, jclass, jobject voiceMap) {
		try {
			COMUser comUser;
			HRESULT hr = S_OK;
			std::vector<Voice*> voices;
			for (int i = 0; i < sizeof(voiceCategories) / sizeof(wchar_t*); i++) {
				hr = addVoices(voiceCategories[i], voices, env);
				if (FAILED(hr)) break;
			}
			buildVoiceMap(voices, env, voiceMap);
		}
		catch (NativeException *e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException /**e*/) {
			// Forwarded automatically
		}
	}

	HRESULT addVoices(const wchar_t* pszCatName, std::vector<Voice*>& voices, JNIEnv *env) {
		CComPtr<IEnumSpObjectTokens> cpEnum;
		HRESULT hr = SpEnumTokens(pszCatName, NULL, NULL, &cpEnum);
		const bool succeeded = SUCCEEDED(hr);
		const bool notAvailable = hr == SPERR_NOT_FOUND;
		assert(succeeded || notAvailable);
		if (notAvailable) return S_FALSE;
		if (succeeded) {
			CComPtr<ISpObjectToken> cpVoiceToken;
			if (SUCCEEDED(hr)) {
				ULONG ulCount = 0;
				hr = cpEnum->GetCount(&ulCount);
				if (SUCCEEDED(hr)) {
					while (ulCount--) {
						hr = cpEnum->Next(1, &cpVoiceToken, NULL);
						if (SUCCEEDED(hr)) {
							voices.push_back(new Voice(env, cpVoiceToken));
							cpVoiceToken.Release();
						}
						else {
							break;
						}
					}
				}
			}
		}
	}

	void buildVoiceMap(const std::vector<Voice*>& voices, JNIEnv *env, jobject voiceMap) {
		jclass mapClass = JNIClass::getClass(env, "java/util/HashMap");
		std::for_each(voices.begin(), voices.end(), [&](const Voice * voice) {
			JNIString key(env, voice->guid.c_str());
			jobject jvoice = *voice;
			env->CallObjectMethod(
				voiceMap,
				env->GetMethodID(mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), key.operator jstring(), jvoice);
			if (env->ExceptionCheck()) {
				throw new JNIException(env);
			}
		});
	}

    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    setVoice
    * Signature: (Lteaselib/texttospeech/Voice;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_setVoice
    (JNIEnv *env, jobject jthis, jobject jvoice) {
        try {
            // Dispose existing
            SpeechSynthesizer* speechSynthesizer = static_cast<SpeechSynthesizer*>(NativeObject::get(env, jthis));
            if (!speechSynthesizer) {
				speechSynthesizer = new SpeechSynthesizer(env, jthis);
            }
            Voice* voice = static_cast<Voice*>(NativeObject::get(env, jvoice));
            // A null voice is a valid value, it denotes the system default voice
			speechSynthesizer->setVoice(voice);
        } catch (NativeException *e) {
            JNIException::throwNew(env, e);
        } catch (JNIException /**e*/) {
            // Forwarded automatically
        }
    }

    jobjectArray getHints(JNIEnv* env, jobject jthis) {
        jclass jclass = env->GetObjectClass(jthis);
        jobjectArray hints = (jobjectArray) env->GetObjectField(jthis, env->GetFieldID(jclass, "hints", "[Ljava/lang/String;"));
        if (env->ExceptionCheck()) {
            throw new JNIException(env);
        }
        return hints;
    }

    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    speak
    * Signature: (Ljava/lang/String;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_speak__Ljava_lang_String_2
    (JNIEnv *env, jobject jthis, jstring prompt) {
        try {
            SpeechSynthesizer* speechSynthesizer = static_cast<SpeechSynthesizer*>(NativeObject::get(env, jthis));
            NativeObject::checkInitializedOrThrow(speechSynthesizer);
            speechSynthesizer->applyHints(JNIUtilities::stringArray(env, getHints(env, jthis)));
            speechSynthesizer->speak(JNIString(env, prompt));
        } catch (NativeException *e) {
            JNIException::throwNew(env, e);
        } catch (JNIException /**e*/) {
            // Forwarded automatically
        }
    }

    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    speak
    * Signature: (Ljava/lang/String;Ljava/lang/String;)V
    */
    JNIEXPORT jstring JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_speak__Ljava_lang_String_2Ljava_lang_String_2
    (JNIEnv *env, jobject jthis, jstring prompt, jstring path) {
        jstring actualPath = NULL;
        try {
            SpeechSynthesizer* speechSynthesizer = static_cast<SpeechSynthesizer*>(NativeObject::get(env, jthis));
            NativeObject::checkInitializedOrThrow(speechSynthesizer);
            speechSynthesizer->applyHints(JNIUtilities::stringArray(env, getHints(env, jthis)));
            std::wstring soundFile = speechSynthesizer->speak(JNIString(env, prompt), JNIString(env, path));
            actualPath = JNIString(env, soundFile.c_str()).detach();
        } catch (NativeException *e) {
            JNIException::throwNew(env, e);
        } catch (JNIException /**e*/) {
            // Forwarded automatically
        }
        return actualPath;
    }


    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    stop
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_stop
    (JNIEnv * env, jobject jthis)
    try {
        SpeechSynthesizer* speechSynthesizer = static_cast<SpeechSynthesizer*>(NativeObject::get(env, jthis));
        NativeObject::checkInitializedOrThrow(speechSynthesizer);
        speechSynthesizer->stop();
    } catch (NativeException *e) {
        JNIException::throwNew(env, e);
    } catch (JNIException /**e*/) {
        // Forwarded automatically
    }

    /*
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    dispose
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_dispose
    (JNIEnv *env, jobject jthis) {
        SpeechSynthesizer* speechSynthesizer = static_cast<SpeechSynthesizer*>(NativeObject::get(env, jthis));
        NativeObject::checkInitializedOrThrow(speechSynthesizer);
        delete speechSynthesizer;
    }
}
