#include "stdafx.h"

#include "JNIException.h"

#include "SpeechRecognitionStartedEvent.h"

SpeechRecognitionStartedEvent::SpeechRecognitionStartedEvent(JNIEnv *env, jobject jsource, const char* name)
    : Event(env, jsource, name)
{}

void SpeechRecognitionStartedEvent::fire() {
    jclass eventClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/events/SpeechRecognitionStartedEventArgs");
    jobject eventArgs = env->NewObject(
                            eventClass,
                            JNIClass::getMethodID(env, eventClass, "<init>", "()V"));
    if (env->ExceptionCheck()) {
        throw new JNIException(env);
    }
    __super::fire(eventArgs);
}
