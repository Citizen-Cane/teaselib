// TeaseLibTTS.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"

#include <algorithm>
#include <vector>

#include <assert.h>

#include <atlbase.h>
#include <sapi.h>

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
    * Class:     teaselib_texttospeech_implementation_TeaseLibTTS
    * Method:    getInstalledVoices
    * Signature: (Ljava/util/Map;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_texttospeech_implementation_TeaseLibTTS_getInstalledVoices
    (JNIEnv *env, jclass, jobject voiceMap) {
        try {
            COMUser cu;
            std::vector<Voice*> voices;
            HRESULT hr = S_OK;
            CComPtr<ISpObjectToken> cpVoiceToken;
            CComPtr<IEnumSpObjectTokens> cpEnum;
            ULONG ulCount = 0;
            // Enumerate the available voices
            CComPtr<ISpObjectTokenCategory> cpCategory;
            CComPtr<ISpObjectTokenCategory> cpTokenCategory;
            hr = cpTokenCategory.CoCreateInstance(CLSID_SpObjectTokenCategory);
            if (SUCCEEDED(hr)) {
                hr = cpTokenCategory->SetId(SPCAT_VOICES, false);
                if (SUCCEEDED(hr)) {
                    cpCategory = cpTokenCategory.Detach();
                    if (SUCCEEDED(hr)) {
                        hr = cpCategory->EnumTokens(NULL, NULL, &cpEnum);
                    }
                }
            }
            if (SUCCEEDED(hr)) {
                // Get the number of voices
                hr = cpEnum->GetCount(&ulCount);
                // Obtain a list of available voice tokens,
                // set the voice to the token, and call Speak
                while (SUCCEEDED(hr) && ulCount--) {
                    cpVoiceToken.Release();
                    if (SUCCEEDED(hr)) {
                        hr = cpEnum->Next(1, &cpVoiceToken, NULL);
                    }
                    voices.push_back(new Voice(env, cpVoiceToken));
                }
            }
            // Build a map from the voices
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
        } catch (NativeException *e) {
            JNIException::throwNew(env, e);
        } catch (JNIException /**e*/) {
            // Forwarded automatically
        }
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
            if (speechSynthesizer) {
                if (env->IsSameObject(*speechSynthesizer, jthis)) {
                    return;
                } else {
                    delete speechSynthesizer;
                }
            }
            Voice* voice = static_cast<Voice*>(NativeObject::get(env, jvoice));
            // A null voice is a valid value, it denotes the system default voice
            // The speech synthesizer object reference will be managed by the jobject,
            // so there's no need to store the reference here
            new SpeechSynthesizer(env, jthis, voice);
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
