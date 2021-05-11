#pragma once

#include "C:\Program Files\Loquendo\LTTS7\include\loqtts.h"

#include "Voice.h"

class LoquendoVoice : public Voice {
public:
	LoquendoVoice(JNIEnv* env, jobject ttsImpl, ttsHandleType hVoice, ttsHandleType hLanguage,
		const char* id, const char* gender, const char* locale, const char* languageDescription);
	~LoquendoVoice();

	ttsResultType applyTo(ttsHandleType hReader) const;

private:
	ttsHandleType hVoice;
	ttsHandleType hLanguage;
};

