#include "stdafx.h"

#include <assert.h>

#include "JNIException.h"

#include "Event.h"


Event::Event(JNIEnv *env, jobject sender, jobject jevent, const char* name)
    : env(env)
    , sender(sender)
    , jevent(jevent)
    , name(name) {
    assert(env);
    assert(sender);
    assert(jevent);
    assert(name);
}


Event::~Event() {
}

void Event::fire(jobject eventArgs) {
    assert(jevent);
    jclass jeventClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/SpeechRecognitionEvents");
    jobject eventSource = env->GetObjectField(
                              jevent,
                              JNIClass::getFieldID(env, jeventClass, name, "Lteaselib/core/events/EventSource;"));
    assert(eventSource);
    env->CallObjectMethod(
        eventSource,
        JNIClass::getMethodID(env, eventSource, "run", "(Ljava/lang/Object;Lteaselib/core/events/EventArgs;)V"),
        sender,
        eventArgs);
    if (env->ExceptionCheck()) {
        throw new JNIException(env);
    }
}
