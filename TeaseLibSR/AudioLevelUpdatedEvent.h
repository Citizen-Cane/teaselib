#pragma once

#include <Event.h>

class AudioLevelUpdatedEvent : public Event {
public:
	AudioLevelUpdatedEvent(JNIEnv *env, jobject jsource, const char* name);
	
	void fire(const int audioLevel);
};

