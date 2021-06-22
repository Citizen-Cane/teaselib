#include "stdafx.h"

#include <JNIUtilities.h>

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

jobject SpeechSynthesizer::voices(jobject jthis)
{
	if (!jvoices) {
		 enumerate_voices(jthis, cached_voices);
		 jvoices = env->NewGlobalRef(JNIUtilities::asList(env, cached_voices));
	}
	return jvoices;
}

void SpeechSynthesizer::setHints(const std::vector<std::wstring>& hints_)
{
	this->hints = hints_;
}
