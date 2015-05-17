#pragma once

#include <Event.h>

class SpeechRecognitionStartedEvent : public Event
{
public:
	SpeechRecognitionStartedEvent(JNIEnv *env, jobject sender, jobject jsource, const char* name);
	virtual ~SpeechRecognitionStartedEvent();

	void fire();
};

