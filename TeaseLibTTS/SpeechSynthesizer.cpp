#include "stdafx.h"

#include <algorithm>

#include <JNIUtilities.h>

#include "SpeechSynthesizer.h"

using namespace std;

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

	for_each(cached_voices.begin(), cached_voices.end(), [] ( NativeObject* voice) { delete voice; });
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
