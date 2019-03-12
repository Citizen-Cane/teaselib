#pragma once

#include <Event.h>

struct ISpRecoResult;

class SpeechRecognizedEvent : public Event
{
public:
	SpeechRecognizedEvent(JNIEnv *env, jobject sender, jobject jevent, const char* name);
	virtual ~SpeechRecognizedEvent();

	void fire(ISpRecoResult* pResult);
private:
	jobject getResult(ISpRecoResult * pResult, const SPPHRASE * pPhrase, const jclass speechRecognitionResultClass);
	static jobject getRule(JNIEnv *env, ISpRecoResult* pResult, const SPPHRASERULE* rule);
	static int choiceIndex(const SPPHRASERULE * rule);
};

