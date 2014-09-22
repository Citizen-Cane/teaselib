#include "stdafx.h"

#include <sapi.h>

#include <assert.h>

#include "COMException.h"

#include "SpeechSynthesizer.h"
#include "Voice.h"

SpeechSynthesizer::SpeechSynthesizer(JNIEnv *env, jobject jthis, Voice *voice)
: NativeObject(env, jthis)
{
	HRESULT hr = CoCreateInstance(CLSID_SpVoice, NULL, CLSCTX_ALL, IID_ISpVoice, (void **)&pVoice);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
	if (voice)
	{
		hr = pVoice->SetVoice(*voice);
		if (FAILED(hr)) throw new COMException(hr);
	}
}


SpeechSynthesizer::~SpeechSynthesizer()
{
	pVoice->Release();
	pVoice = NULL;
}

void SpeechSynthesizer::speak(const wchar_t *prompt)
{
	HRESULT hr = pVoice->Speak(prompt, 0, NULL);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
}