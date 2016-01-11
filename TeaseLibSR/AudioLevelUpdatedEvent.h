#pragma once

#include <Event.h>

class AudioLevelUpdatedEvent : public Event
{
public:
	AudioLevelUpdatedEvent(JNIEnv *env, jobject sender, jobject jsource, const char* name);
	virtual ~AudioLevelUpdatedEvent();

	void fire(const int audioLevel);
};

