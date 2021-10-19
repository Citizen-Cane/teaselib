#include "stdafx.h"

#include <string>

#include <JNIString.h>

#include "LoquendoVoice.h"

using namespace std;

LoquendoVoice::LoquendoVoice(JNIEnv* env, jobject ttsImpl, ttsHandleType hVoice, ttsHandleType hLanguage,
	const char* id, const char* gender, const char* locale, const char* languageName)
	: Voice(env)
	, hVoice(hVoice), hLanguage(hLanguage)
{
	string guid = string("LTTS7") + id;
	jobject jgender = getGenderField(gender);
	jobject jvoiceInfo = newVoiceInfo(JNIStringUTF8(env, "Loquendo"), JNIStringUTF8(env, languageName), JNIStringUTF8(env, id));
	jthis = env->NewGlobalRef(newNativeVoice(ttsImpl, JNIStringUTF8(env, guid), jgender, JNIStringUTF8(env, locale), jvoiceInfo));
}

LoquendoVoice::~LoquendoVoice()
{
	// hVoice and hLanguage are implicitely deleted in ttsDeleteSEssion() in LoquendoSpeechSynthesizer::dispose()
}

ttsResultType LoquendoVoice::applyTo(ttsHandleType hReader) const
{
	ttsResultType result =  ttsSetVoice(hReader, hVoice);
	if (result == tts_OK) {
		result = ttsSetLanguage(hReader, hLanguage);
	}
	return result;
}
