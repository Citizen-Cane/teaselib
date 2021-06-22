#pragma once

#include <atomic>
#include <string>
#include <vector>

#include <jni.h>

enum SPPARTOFSPEECH;
class Voice;

class SpeechSynthesizer {
public:
	SpeechSynthesizer(JNIEnv* env);
	virtual ~SpeechSynthesizer();

	jobject voices(jobject jthis);

	virtual void addLexiconEntry(const wchar_t* const  locale, const wchar_t* const  word, const SPPARTOFSPEECH partOfSpeech, const wchar_t* const pronunciation) = 0;
	virtual void setVoice(const Voice* voice) = 0;
	virtual void setHints(const std::vector<std::wstring>& hints);
	virtual void speak(const wchar_t* prompt) = 0;
	virtual std::wstring speak(const wchar_t* prompt, const wchar_t* path) = 0;
	virtual void stop() = 0;

protected:
	JNIEnv* env;
	jobject jvoices;
	std::vector<NativeObject*> cached_voices;
	virtual void enumerate_voices(jobject jthis, std::vector<NativeObject*>& voices) = 0;

	std::vector<std::wstring> hints;
	std::atomic_bool cancelSpeech;
};
