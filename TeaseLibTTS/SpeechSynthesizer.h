#pragma once

#include <vector>

#include <NativeObject.h>
#include <COMUser.h>

struct ISpVoice;

class Voice;

class SpeechSynthesizer : public NativeObject, protected COMUser {
public:
    SpeechSynthesizer(JNIEnv *env, jobject jthis, Voice *voice);
    virtual ~SpeechSynthesizer();

	void applyHints(const std::vector<std::wstring>& hints);
    void speak(const wchar_t *prompt);
    std::wstring speak(const wchar_t *prompt, const wchar_t *path);
    void stop();

private:
    ISpVoice *pVoice;
	volatile bool cancelSpeech;
};
