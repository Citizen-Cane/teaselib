#pragma once

#include <Event.h>

class SpeechRecognitionStartedEvent : public Event
{
public:
	SpeechRecognitionStartedEvent(JNIEnv *env, jobject jsource, const char* name);
	
	void fire();
};

