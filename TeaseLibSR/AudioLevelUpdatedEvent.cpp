#include "stdafx.h"

#include "JNIException.h"

#include "AudioLevelUpdatedEvent.h"

AudioLevelUpdatedEvent::AudioLevelUpdatedEvent(JNIEnv *env, jobject sender, jobject jsource, const char* name)
: Event(env, sender, jsource, name)
{
}

AudioLevelUpdatedEvent::~AudioLevelUpdatedEvent()
{
}

void AudioLevelUpdatedEvent::fire(const int audioLevel)
{
	// TODO Implement
	jclass eventClass = JNIClass::getClass(env, "teaselib/speechrecognition/events/AudioLevelUpdatedEventArgs");
	jobject eventArgs = env->NewObject(
		eventClass,
		JNIClass::getMethodID(env, eventClass, "<init>", "(I)V"),
		static_cast<jint>(audioLevel));
	if (env->ExceptionCheck()) throw new JNIException(env);
	__super::fire(eventArgs);
}
