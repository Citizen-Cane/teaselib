#include "stdafx.h"

#include "JNIException.h"

#include "AudioLevelSignalProblemOccuredEvent.h"

AudioLevelSignalProblemOccuredEvent::AudioLevelSignalProblemOccuredEvent(JNIEnv *env, jobject sender, jobject jevent, const char* name)
    : Event(env, sender, jevent, name) {
}

AudioLevelSignalProblemOccuredEvent::~AudioLevelSignalProblemOccuredEvent() {
}

void AudioLevelSignalProblemOccuredEvent::fire(const SPINTERFERENCE interference) {
    const char* audioSignalProblemFieldName;
    //public enum AudioSignalProblem
    //{
    //  Noise,
    //  NoSignal,
    //  TooLoud,
    //  TooQuiet,
    //  TooFast,
    //  TooSlow
    //}
    if (interference == SPINTERFERENCE_NONE) {
        audioSignalProblemFieldName = "None";
    } else if (interference & SPINTERFERENCE_NOISE) {
        audioSignalProblemFieldName = "Noise";
    } else if (interference & SPINTERFERENCE_NOSIGNAL) {
        audioSignalProblemFieldName = "NoSignal";
    } else if (interference & SPINTERFERENCE_TOOLOUD) {
        audioSignalProblemFieldName = "TooLoud";
    } else if (interference & SPINTERFERENCE_TOOQUIET) {
        audioSignalProblemFieldName = "TooQuiet";
    } else if (interference & SPINTERFERENCE_TOOFAST) {
        audioSignalProblemFieldName = "TooFast";
    } else if (interference & SPINTERFERENCE_TOOSLOW) {
        audioSignalProblemFieldName = "TooSlow";
    } else {
        assert(false);
        return;
    }
    jclass audioSignalProblemClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/SpeechRecognition$AudioSignalProblem");
    jobject audioSignalProblem = env->GetStaticObjectField(
                                     audioSignalProblemClass,
                                     JNIClass::getStaticFieldID(env, audioSignalProblemClass, audioSignalProblemFieldName,
                                             "Lteaselib/core/speechrecognition/SpeechRecognition$AudioSignalProblem;"));
    jclass eventClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/events/AudioSignalProblemOccuredEventArgs");
    jobject eventArgs = env->NewObject(
                            eventClass,
                            JNIClass::getMethodID(env, eventClass, "<init>", "(Lteaselib/core/speechrecognition/SpeechRecognition$AudioSignalProblem;)V"),
                            audioSignalProblem);
    if (env->ExceptionCheck()) {
        throw new JNIException(env);
    }
    __super::fire(eventArgs);
}
