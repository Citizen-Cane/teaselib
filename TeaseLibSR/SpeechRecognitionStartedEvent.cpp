#include "stdafx.h"

#include "JNIException.h"

#include "SpeechRecognitionStartedEvent.h"

SpeechRecognitionStartedEvent::SpeechRecognitionStartedEvent(JNIEnv *env, jobject sender, jobject jsource, const char* name)
    : Event(env, sender, jsource, name) {
}

SpeechRecognitionStartedEvent::~SpeechRecognitionStartedEvent() {
}

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
