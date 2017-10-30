#pragma once

#include <vector>

#include <NativeObject.h>
#include <COMUser.h>
#include <Lexicon.h>

struct ISpVoice;

class Voice;

class SpeechSynthesizer : public NativeObject, private COMUser {
public:
    SpeechSynthesizer(JNIEnv *env, jobject ttsImpl);
    virtual ~SpeechSynthesizer();

	jobject voiceList();
	HRESULT addVoices(const wchar_t* pszCatName, std::vector<Voice*>& voices);

	void addLexiconEntry(const wchar_t * const  locale, const wchar_t * const  word, const SPPARTOFSPEECH partOfSpeech, const wchar_t * const  pronunciation);
	void setVoice(const Voice *voice);
	void applyHints(const std::vector<std::wstring>& hints);
    void speak(const wchar_t *prompt);
    std::wstring speak(const wchar_t *prompt, const wchar_t *path);
    void stop();

private:
	jobject jvoiceList(const std::vector<Voice*>& voices);

    ISpVoice *pVoice;
	volatile bool cancelSpeech;

	Lexicon lexicon;
	jobject jvoices;
};
