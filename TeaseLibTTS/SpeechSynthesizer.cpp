#include "stdafx.h"
#include "SpeechSynthesizer.h"

SpeechSynthesizer::SpeechSynthesizer(JNIEnv* env)
	: env(env)
	, jvoices(nullptr)
	, cancelSpeech(false)
{}

SpeechSynthesizer::~SpeechSynthesizer()
{
	if (jvoices) {
		env->DeleteGlobalRef(jvoices);
	}
}

jobject SpeechSynthesizer::voices(jobject jthis) {
	if (!jvoices) {
		jvoices = env->NewGlobalRef(enumerate_voices(jthis));
	}
	return jvoices;
}

void SpeechSynthesizer::setHints(const std::vector<std::wstring>& hints)
{
	this->hints = hints;
}
