// TeaseLibSR.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"

#include <map>
#include <string>

#include <assert.h>

#include <atlbase.h>

#include <COMException.h>
#include <COMUser.h>
#include <JNIException.h>
#include <JNIString.h>
#include <JNIObject.h>
#include <NativeException.h>
#include <NativeObject.h>

#include <teaselib_core_speechrecognition_sapi_TeaseLibSR.h>
#include "SpeechRecognizer.h"

using namespace std;

extern "C"
{
	/*
	 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
	 * Method:    initSR
	 * Signature: (Ljava/lang/String;)V
	 */
    JNIEXPORT jlong JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_newNativeInstance
    (JNIEnv *env, jclass, jstring jlanguageCode) {
        try {
            Objects::requireNonNull(L"locale", jlanguageCode);
            JNIString languageCode(env, jlanguageCode);
            SpeechRecognizer* speechRecognizer = new SpeechRecognizer(env, languageCode);
            if (speechRecognizer == nullptr) throw COMException(E_OUTOFMEMORY);
            return reinterpret_cast<jlong>(speechRecognizer);
        } catch (NativeException& e) {
            JNIException::rethrow(env, e);
            return 0;
        } catch (JNIException& e) {
			e.rethrow();
            return 0;
		}
    }


	/*
	 * Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
	 * Method:    initSREventThread
	 * Signature: (Lteaselib/core/speechrecognition/SpeechRecognitionEvents;Ljava/util/concurrent/CountDownLatch;)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_process
    (JNIEnv *env, jobject jthis, jobject jevents, jobject jSignalInitialized) {
        try {
            Objects::requireNonNull(L"signalInitialized", jSignalInitialized);
            SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
			const std::function<void(void)> signalInitialized = [env, jSignalInitialized]() {
				env->CallVoidMethod(jSignalInitialized, env->GetMethodID(env->GetObjectClass(jSignalInitialized), "countDown", "()V"));
			};
			speechRecognizer->startEventHandler(env,  jthis, jevents, signalInitialized);
        } catch (NativeException& e) {
            JNIException::rethrow(env, e);
        } catch (JNIException& e) {
			e.rethrow();
        }
    }

    /*
     * Class:     teaselib_core_speechrecognition_sapi_TeaseLibSR
     * Method:    languageCode
     * Signature: ()Ljava/lang/String;
     */
    JNIEXPORT jstring JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_languageCode
    (JNIEnv *env, jobject jthis) {
        try {
            SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
            return JNIString(env, speechRecognizer->locale);
        }
        catch (NativeException& e) {
            JNIException::rethrow(env, e);
            return nullptr;
        }
        catch (JNIException& e) {
            e.rethrow();
            return nullptr;
        }
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    startRecognition
    * Signature: (Ljava/util/Map;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_setChoices__Ljava_util_List_2
    (JNIEnv *env, jobject jthis, jobject jchoices) {
		try {
            Objects::requireNonNull(L"choices", jchoices);
            SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
            jclass listClass = JNIClass::getClass(env, "Ljava/util/List;");
            jobject iterator = env->CallObjectMethod(jchoices, env->GetMethodID(listClass, "iterator", "()Ljava/util/Iterator;"));
            jclass iteratorClass = env->FindClass("Ljava/util/Iterator;");
            if (env->ExceptionCheck()) {
                throw JNIException(env);
            }
            SpeechRecognizer::Choices choices;
            while (env->CallBooleanMethod(iterator, env->GetMethodID(iteratorClass, "hasNext", "()Z"))) {
                jobject jchoice = env->CallObjectMethod(iterator, env->GetMethodID(iteratorClass, "next", "()Ljava/lang/Object;"));
                if (env->ExceptionCheck()) {
                    throw JNIException(env);
                }
                JNIString choice(env, reinterpret_cast<jstring>(jchoice));
                choices.push_back(wstring(choice));
            }
            speechRecognizer->setChoices(choices);
        } catch (NativeException& e) {
            JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
    }

	/*
* Class:     teaselib_core_speechrecognition_implementation_TeaseLibSR
* Method:    setChoices
* Signature: (Ljava/lang/String;)V
*/
	JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_setChoices___3B
	(JNIEnv* env , jobject jthis , jbyteArray jsrgs) {
	try {
            Objects::requireNonNull(L"srgs", jsrgs);
            SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
			jboolean isCopy;
			const BYTE* srgs = reinterpret_cast<const BYTE*>(env->GetByteArrayElements(jsrgs, &isCopy));
			CComPtr<IStream> stream = SHCreateMemStream(reinterpret_cast<const BYTE*>(srgs), env->GetArrayLength(jsrgs));
			if (!stream) throw COMException(E_OUTOFMEMORY);

			speechRecognizer->setChoices(stream);
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
	}


    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    setMaxAlternates
    * Signature: (I)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_setMaxAlternates
    (JNIEnv *env, jobject jthis, jint maxAlternates) {
        try {
            SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
            speechRecognizer->setMaxAlternates(maxAlternates);
        } catch (NativeException& e) {
            JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    startRecognition
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_startRecognition
    (JNIEnv *env, jobject jthis) {
        try {
            SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
            speechRecognizer->startRecognition();
        } catch (NativeException& e) {
            JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    emulateRecognition
    * Signature: (Ljava/lang/String;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_emulateRecognition
    (JNIEnv *env, jobject jthis, jstring emulatedRecognitionResult) {
		try {
            Objects::requireNonNull(L"emulatedRecognitionResult", emulatedRecognitionResult);
            SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
            speechRecognizer->emulateRecognition(JNIString(env,emulatedRecognitionResult));
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    stopRecognition
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_stopRecognition
    (JNIEnv *env, jobject jthis) {
        try {
            SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
            speechRecognizer->stopRecognition();
        } catch (NativeException& e) {
            JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
    }

    /*
 * Class:     teaselib_core_speechrecognition_sapi_TeaseLibSR
 * Method:    interruptNativeEventHandler
 * Signature: ()V
 */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_stopEventLoop
    (JNIEnv* env, jobject jthis)
    {
        try {
            SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
            speechRecognizer->stopEventLoop();
        }
        catch (NativeException& e) {
            JNIException::rethrow(env, e);
        }
        catch (JNIException& e) {
            e.rethrow();
        }
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    dispose
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_sapi_TeaseLibSR_dispose
    (JNIEnv *env, jobject jthis) {
        SpeechRecognizer* speechRecognizer = NativeInstance::get<SpeechRecognizer>(env, jthis);
        delete speechRecognizer;
    }

}
