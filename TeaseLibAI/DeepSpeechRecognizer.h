#pragma once

#include <string>
#include <vector>

#include <NativeObject.h>

#include <Audio/AIfxAudioCapture.h>
#include <Audio/AIfxSpeechAudioStream.h>

#include <DeepSpeech/AIfxDeepSpeech.h>
#include <DeepSpeech/AIfxDeepSpeechAudioStreamRecognizer.h>


class DeepSpeechRecognizer {
	aifx::DeepSpeechAudioStreamRecognizer recognizer;
	aifx::SpeechAudioStream audioStream;
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

	aifx::SpeechAudioStream::Status decode();
	const std::vector<aifx::DeepSpeech::Recognition> results() const;

	static const jobject jresults(JNIEnv* env, const std::vector<aifx::DeepSpeech::Recognition>& results);
};
