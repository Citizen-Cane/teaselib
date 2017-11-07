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
#include <NativeException.h>
#include <NativeObject.h>

#include "SpeechRecognizer.h"

#include <teaselib_core_speechrecognition_implementation_TeaseLibSR.h>

using namespace std;

extern "C"
{
    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    initSR
    * Signature: (Lteaselib/speechrecognition/SpeechRecognitionEvents;Ljava/lang/String;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_initSR
    (JNIEnv *env, jobject jthis, jobject jevents, jstring jlocale) {
        try {
            // TODO Language
            JNIString locale(env, jlocale);
            // TODO Pass language code instead of recognizer attribute
            SpeechRecognizer* speechRecognizer = new SpeechRecognizer(env, jthis, jevents, locale);
            NativeObject::checkInitializedOrThrow(speechRecognizer);
        } catch (NativeException *e) {
            JNIException::throwNew(env, e);
        } catch (JNIException * /**e*/) {
            // Forwarded automatically
        }
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    initSREventThread
    * Signature: (Lteaselib/speechrecognition/implementation/TeaseLibSR;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_initSREventThread
    (JNIEnv *threadEnv, jobject jthis, jobject jthread) {
        try {
            SpeechRecognizer* speechRecognizer = static_cast<SpeechRecognizer*>(NativeObject::get(threadEnv, jthis));
            NativeObject::checkInitializedOrThrow(speechRecognizer);
            COMUser([&speechRecognizer, &threadEnv, &jthis, &jthread]() {
                speechRecognizer->speechRecognitionInitContext();

				threadEnv->CallVoidMethod(jthread, threadEnv->GetMethodID(threadEnv->GetObjectClass(jthread), "notifyAll", "()V"));
                if (threadEnv->ExceptionCheck()) {
                    throw new JNIException(threadEnv);
                }

                // notifyAll() doesn't cause the waiting threads to continue immediately,
                // first the thread that obtains the monitor (that's us) has to leave the synchronized block
                // We have to do it manually here, since
                // the thread never leaves the synchronized() context that was established in TeaseLibSR.init()
                threadEnv->MonitorExit(jthread);
                if (threadEnv->ExceptionCheck()) {
                    throw new JNIException(threadEnv);
                }
                speechRecognizer->speechRecognitionEventHandlerThread(threadEnv);
            });
        } catch (NativeException *e) {
            JNIException::throwNew(threadEnv, e);
        } catch (JNIException * /**e*/) {
            // Forwarded automatically
        }
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    startRecognition
    * Signature: (Ljava/util/Map;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_setChoices
    (JNIEnv *env, jobject jthis, jobject jchoices) {
        try {
            SpeechRecognizer* speechRecognizer = static_cast<SpeechRecognizer*>(NativeObject::get(env, jthis));
            NativeObject::checkInitializedOrThrow(speechRecognizer);
            // jchoices is a set
            jclass listClass = JNIClass::getClass(env, "Ljava/util/List;");
            jobject iterator = env->CallObjectMethod(jchoices, env->GetMethodID(listClass, "iterator", "()Ljava/util/Iterator;"));
            jclass iteratorClass = env->FindClass("Ljava/util/Iterator;");
            if (env->ExceptionCheck()) {
                throw new JNIException(env);
            }
            SpeechRecognizer::Choices choices;
            while (env->CallBooleanMethod(iterator, env->GetMethodID(iteratorClass, "hasNext", "()Z"))) {
                jobject jchoice = env->CallObjectMethod(iterator, env->GetMethodID(iteratorClass, "next", "()Ljava/lang/Object;"));
                if (env->ExceptionCheck()) {
                    throw new JNIException(env);
                }
                JNIString choice(env, reinterpret_cast<jstring>(jchoice));
                choices.push_back(wstring(choice));
            }
            speechRecognizer->setChoices(choices);
        } catch (NativeException *e) {
            JNIException::throwNew(env, e);
        } catch (JNIException * /**e*/) {
            // Forwarded automatically
        }
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    setMaxAlternates
    * Signature: (I)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_setMaxAlternates
    (JNIEnv *env, jobject jthis, jint maxAlternates) {
        try {
            SpeechRecognizer* speechRecognizer = static_cast<SpeechRecognizer*>(NativeObject::get(env, jthis));
            NativeObject::checkInitializedOrThrow(speechRecognizer);
            speechRecognizer->setMaxAlternates(maxAlternates);
        } catch (NativeException *e) {
            JNIException::throwNew(env, e);
        } catch (JNIException /**e*/) {
            // Forwarded automatically
        }
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    startRecognition
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_startRecognition
    (JNIEnv *env, jobject jthis) {
        try {
            SpeechRecognizer* speechRecognizer = static_cast<SpeechRecognizer*>(NativeObject::get(env, jthis));
            NativeObject::checkInitializedOrThrow(speechRecognizer);
            speechRecognizer->startRecognition();
        } catch (NativeException *e) {
            JNIException::throwNew(env, e);
        } catch (JNIException /**e*/) {
            // Forwarded automatically
        }
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    emulateRecognition
    * Signature: (Ljava/lang/String;)V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_emulateRecognition
    (JNIEnv *env, jobject jthis, jstring emulatedRecognitionResult) {
		try {
			SpeechRecognizer* speechRecognizer = static_cast<SpeechRecognizer*>(NativeObject::get(env, jthis));
			NativeObject::checkInitializedOrThrow(speechRecognizer);

			speechRecognizer->emulateRecognition(JNIString(env,emulatedRecognitionResult));
		}
		catch (NativeException *e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException /**e*/) {
			// Forwarded automatically
		}
	}

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    stopRecognition
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_stopRecognition
    (JNIEnv *env, jobject jthis) {
        try {
            SpeechRecognizer* speechRecognizer = static_cast<SpeechRecognizer*>(NativeObject::get(env, jthis));
            NativeObject::checkInitializedOrThrow(speechRecognizer);
            speechRecognizer->stopRecognition();
        } catch (NativeException *e) {
            JNIException::throwNew(env, e);
        } catch (JNIException /**e*/) {
            // Forwarded automatically
        }
    }

    /*
    * Class:     teaselib_speechrecognition_implementation_TeaseLibSR
    * Method:    dispose
    * Signature: ()V
    */
    JNIEXPORT void JNICALL Java_teaselib_core_speechrecognition_implementation_TeaseLibSR_dispose
    (JNIEnv *env, jobject jthis) {
        SpeechRecognizer* speechRecognizer = static_cast<SpeechRecognizer*>(NativeObject::get(env, jthis));
        NativeObject::checkInitializedOrThrow(speechRecognizer);
        delete speechRecognizer;
    }
}
