#pragma once

#include <Event.h>

#include <sapi.h>

class AudioLevelSignalProblemOccuredEvent : public Event {
public:
	AudioLevelSignalProblemOccuredEvent(JNIEnv *env, jobject jevent, const char* name);

	void fire(const SPINTERFERENCE interference);
};

