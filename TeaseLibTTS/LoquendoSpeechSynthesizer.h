#pragma once

#include "C:\Program Files\Loquendo\LTTS7\include\loqtts.h"

#include "SpeechSynthesizer.h"

class LoquendoSpeechSynthesizer : public SpeechSynthesizer {
public:
	static const char* Sdk;

	LoquendoSpeechSynthesizer(JNIEnv* env);
	~LoquendoSpeechSynthesizer();

	void addLexiconEntry(const wchar_t* const  locale, const wchar_t* const  word, const SPPARTOFSPEECH partOfSpeech, const wchar_t* const pronunciation) override;
	void setVoice(const Voice* voice) override;
	void speak(const wchar_t* prompt) override;
	std::wstring speak(const wchar_t* prompt, const wchar_t* path) override;
	void stop();

protected:

private: 
	const HMODULE loquendoLibrary;

	ttsHandleType hSession;
	ttsHandleType hReader;

	void enumerate_voices(jobject jthis, std::vector<NativeObject*>& voices) override;

	enum class OutputType {
		None, AudioDevice, File
	};
	void setOutput(OutputType output, const char* wav = nullptr);
	OutputType current;
	void applyHints();
	void speak(const std::wstring& prompt, ttsBoolType asynchronous);
	void awaitDone();

	std::string extractLocale(const std::string& languageAliases);

	std::string queryVoiceAttribute(const char* id, const char* attribute);
	std::string queryLanguageAttribute(const char* id, const char* attribute);
	const char* queryAttribute(ttsQueryType queryType, const char* id, const char* attribute);

	void checkResult(ttsResultType r);
	void dispose();
};
