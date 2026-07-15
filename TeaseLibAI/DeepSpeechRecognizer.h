#pragma once

#include <filesystem>
#include <set>
#include <string>
#include <vector>

#include <NativeObject.h>

#include <Audio/AudioCapture.h>
#include <Compute/AllocationPlan.h>
#include <Speech/SpeechAudioStream.h>

#include <Whisper/Silero.h>
#include <Whisper/WhisperHypothesizingContext.h>

class DeepSpeechRecognizer {
	aifx::speech::WhisperHypothesizingContext recognizer;
	aifx::speech::vad::Silero vad;
	aifx::speech::SpeechAudioStream audioStream;
	aifx::audio::AudioCapture audio;
	aifx::audio::AudioCapture::Input input;

public:
	DeepSpeechRecognizer(const aifx::compute::AllocationPlan& plan, const char* languageCode);
	~DeepSpeechRecognizer();

	const std::string& languageCode() const;
	void setHotWords(const std::set<std::string>& words, const std::vector<std::string>& phrases);

	void start();
	void stop();
	void emulate(const char* speech);
	void emulate(const std::vector<float>& speech);
	void emulate(const float* speech, unsigned int samples);

	void stopEventLoop();

	aifx::speech::SpeechAudioStream::Status decode();
	const std::vector<aifx::speech::RecognitionResult> results() const;

	static const jobject jresults(JNIEnv* env, const std::vector<aifx::speech::RecognitionResult>& results);
};
