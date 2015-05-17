#pragma once

#include <Event.h>

struct ISpRecoResult;

class SpeechRecognizedEvent : public Event
{
public:
	SpeechRecognizedEvent(JNIEnv *env, jobject sender, jobject jevent, const char* name);
	virtual ~SpeechRecognizedEvent();

	void fire(ISpRecoResult* pResult);
};

