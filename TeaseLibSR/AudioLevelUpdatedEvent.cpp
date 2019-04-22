#include "stdafx.h"

#include "JNIException.h"

#include "AudioLevelUpdatedEvent.h"

AudioLevelUpdatedEvent::AudioLevelUpdatedEvent(JNIEnv *env, jobject jsource, const char* name)
    : Event(env,  jsource, name)
{}

void AudioLevelUpdatedEvent::fire(const int audioLevel) {
    // TODO Implement
    jclass eventClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/events/AudioLevelUpdatedEventArgs");
    jobject eventArgs = env->NewObject(
                            eventClass,
                            JNIClass::getMethodID(env, eventClass, "<init>", "(I)V"),
                            static_cast<jint>(audioLevel));
    if (env->ExceptionCheck()) {
        throw new JNIException(env);
    }
    __super::fire(eventArgs);
}
