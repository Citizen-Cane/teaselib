#pragma once

#include <string>
#include <vector>

#include <NativeObject.h>

#include <AIfxAudioCapture.h>
#include <AIfxDeepSpeech.h>
#include <AIfxDeepSpeechAudioStream.h>


class DeepSpeechRecognizer {
	aifx::DeepSpeechAudioStream recognizer;
	aifx::AudioCapture audio;
	aifx::AudioCapture::Input input;
public:
	DeepSpeechRecognizer(const char* path, const char* languageCode);
	~DeepSpeechRecognizer();

	const std::string& languageCode() const;
	void start();
	void stop();
	void emulate(const char* speech);
	void emulate(const short* speech, unsigned int samples);

	void stopEventLoop();

	aifx::DeepSpeechAudioStream::Status decode();
	const std::vector<aifx::DeepSpeech::Recognition> results() const;

	static const jobject jresults(JNIEnv* env, const std::vector<aifx::DeepSpeech::Recognition>& results);
};

