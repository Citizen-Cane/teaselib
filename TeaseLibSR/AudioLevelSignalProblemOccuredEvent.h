#pragma once

#include <Event.h>

#include <sapi.h>

class AudioLevelSignalProblemOccuredEvent : public Event
{
public:
	AudioLevelSignalProblemOccuredEvent(JNIEnv *env, jobject sender, jobject jevent, const char* name);
	virtual ~AudioLevelSignalProblemOccuredEvent();

	void fire(const SPINTERFERENCE interference);
};

