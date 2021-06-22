#pragma once

#include <vector>

#include <jni.h>

#include <NativeObject.h>
#include <COMUser.h>
#include <Lexicon.h>

#include "SpeechSynthesizer.h"

struct ISpVoice;
class Voice;

class SpSpeechSynthesizer : public SpeechSynthesizer,  private COMUser {
public:
	static const char* Sdk;

    SpSpeechSynthesizer(JNIEnv* env);
    virtual ~SpSpeechSynthesizer();

	void addLexiconEntry(const wchar_t * const  locale, const wchar_t * const  word, const SPPARTOFSPEECH partOfSpeech, const wchar_t * const pronunciation) override;
	void setVoice(const Voice *voice) override;
	void setHints(const std::vector<std::wstring>& hints) override;
    void speak(const wchar_t *prompt) override;
    std::wstring speak(const wchar_t *prompt, const wchar_t *path) override;
    void stop();

private:
	void enumerate_voices(jobject jthis, std::vector<NativeObject*>& voices) override;
	HRESULT addVoices(jobject jthis, const wchar_t* pszCatName, std::vector<NativeObject*>& voices);

	ISpVoice *pVoice;
	Lexicon lexicon;
};

