#pragma once

#include <set>
#include <string>
#include <vector>

#include <NativeObject.h>

#include <Audio/AudioCapture.h>
#include <Speech/SpeechAudioStream.h>

#include <DeepSpeech/DeepSpeechContext.h>
#include <DeepSpeech/DeepSpeechRecognizer.h>


class DeepSpeechRecognizer {
	aifx::speech::DeepSpeechRecognizer recognizer;
	aifx::speech::SpeechAudioStream audioStream;
	aifx::audio::AudioCapture audio;
	aifx::audio::AudioCapture::Input input;
public:
	DeepSpeechRecognizer(const char* path, const char* languageCode);
	~DeepSpeechRecognizer();

	const std::string& languageCode() const;
	void setMaxAlternates(int n);
	void setHotWords(const std::set<std::string>& words);

	void start();
	void stop();
	void emulate(const char* speech);
	void emulate(const short* speech, unsigned int samples);

	void stopEventLoop();

	aifx::speech::SpeechAudioStream::Status decode();
	const std::vector<aifx::speech::RecognitionResult> results() const;

	static const jobject jresults(JNIEnv* env, const std::vector<aifx::speech::RecognitionResult>& results);
};
