#pragma once

#include <NativeObject.h>
#include <COMUser.h>

struct ISpVoice;

class Voice;

class SpeechSynthesizer : public NativeObject, protected COMUser
{
public:
	SpeechSynthesizer(JNIEnv *env, jobject jthis, Voice *voice);
	virtual ~SpeechSynthesizer();

	void speak(const wchar_t *prompt);

private:
	ISpVoice *pVoice;
};
