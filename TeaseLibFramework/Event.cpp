#include "stdafx.h"

#include <assert.h>

#include "JNIException.h"

#include "Event.h"


Event::Event(JNIEnv *env, jobject jevent, const char* name)
    : env(env)
    , jevent(jevent)
    , name(name) {
    assert(env);
    assert(jevent);
    assert(name);
}

Event::~Event()
{}

void Event::fire(jobject eventArgs) {
    assert(jevent);
    jclass jeventClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/SpeechRecognitionEvents");
    jobject eventSource = env->GetObjectField(
		jevent,
        JNIClass::getFieldID(env, jeventClass, name, "Lteaselib/core/events/EventSource;"));
    assert(eventSource);

    env->CallObjectMethod(
        eventSource,
        JNIClass::getMethodID(env, eventSource, "run", "(Lteaselib/core/events/EventArgs;)V"),
         eventArgs);
    if (env->ExceptionCheck()) throw JNIException(env);
}
