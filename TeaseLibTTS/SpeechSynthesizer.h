#pragma once

#include <vector>

#include <jni.h>

#include <NativeObject.h>
#include <COMUser.h>
#include <Lexicon.h>

struct ISpVoice;

class Voice;

class SpeechSynthesizer : private COMUser {
public:
    SpeechSynthesizer();
    virtual ~SpeechSynthesizer();

	jobject voices(JNIEnv* env, jobject jthis);
	HRESULT addVoices(JNIEnv* env, jobject jthis, const wchar_t* pszCatName, std::vector<NativeObject*>& voices);

	void addLexiconEntry(const wchar_t * const  locale, const wchar_t * const  word, const SPPARTOFSPEECH partOfSpeech, const wchar_t * const  pronunciation);
	void setVoice(const Voice *voice);
	void applyHints(const std::vector<std::wstring>& hints);
    void speak(const wchar_t *prompt);
    std::wstring speak(const wchar_t *prompt, const wchar_t *path);
    void stop();

private:
    ISpVoice *pVoice;
	volatile bool cancelSpeech;

	Lexicon lexicon;
	jobject jvoices;
};
