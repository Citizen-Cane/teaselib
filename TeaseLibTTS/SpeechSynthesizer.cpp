#include "stdafx.h"

#include <algorithm>

#include <sapi.h>

#include <assert.h>

#include <sphelper.h>

#include "COMException.h"

#include "SpeechSynthesizer.h"
#include "Voice.h"

using namespace std;

// The debug version speaks very fast without a mood hint, in order to be able to distinguish mood hints from neutral
// The release version values are chosen to be barely noticable, they're supposed to be subtile

#ifdef _DEBUG
	int volumeNeutral = 40;
	int volumeReading = volumeNeutral + 60;
	int rateNeutral = 10;
	int rateReading = -5;
#else
	int volumeNeutral = 50;
	int volumeReading = volumeNeutral + 10;
	int rateNeutral = 0;
	int rateReading = -2;
#endif

SpeechSynthesizer::SpeechSynthesizer(JNIEnv *env, jobject jthis, Voice *voice)
    : NativeObject(env, jthis) {
    HRESULT hr = CoCreateInstance(CLSID_SpVoice, NULL, CLSCTX_ALL, IID_ISpVoice, (void **)&pVoice);
    assert(SUCCEEDED(hr));
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
    if (voice) {
        hr = pVoice->SetVoice(*voice);
        if (FAILED(hr)) {
            throw new COMException(hr);
        }
		else
		{
			hr = pVoice->SetVolume(volumeNeutral);
			if (FAILED(hr)) {
				throw new COMException(hr);
			}
		}
    }
}


SpeechSynthesizer::~SpeechSynthesizer() {
    pVoice->Release();
    pVoice = NULL;
}

// Well, I didn't have much luck with the XML tags,
// because I couldn't reset values in the postfix part of the final prompt
// This may be related to the Loquendo voices I've been using during the hints development,
// but nevertheless prooved unreliable while setting values via COM worked. Good luck!
wstring hintsPromptPrefix;
wstring hintsPromptPostfix;

void SpeechSynthesizer::applyHints(const vector<wstring>& hints)
{
	hintsPromptPrefix.empty();
	hintsPromptPostfix.empty();
	// Changing the volume via xml tags didn't work as expected, because for a fraction of a second the voice sounded at normal volume
	bool volumeChanged = false;
	bool rateChanged = false;
	for_each(hints.begin(), hints.end(), [&](const wstring& hint)
	{
		HRESULT hr = S_OK;
		if (hint.compare(L"<mood=reading>") == 0)
		{
			hr = pVoice->SetVolume(volumeReading);
			if (SUCCEEDED(hr))
			{
				volumeChanged = true;
				hr = pVoice->SetRate(rateReading);
				if (SUCCEEDED(hr))
				{
					rateChanged = true;
				}
			}
		}
		if (FAILED(hr)) {
			throw new COMException(hr);
		}
	});
	// Defaults
	if (!volumeChanged)
	{
		HRESULT hr = pVoice->SetVolume(volumeNeutral);
		if (FAILED(hr)) {
			throw new COMException(hr);
		}
	}
	if (!rateChanged)
	{
		HRESULT hr = pVoice->SetRate(rateNeutral);
		if (FAILED(hr)) {
			throw new COMException(hr);
		}
	}
}

wstring createPromptWitthHints(const wchar_t * prompt)
{
	return (hintsPromptPrefix + prompt + hintsPromptPostfix);
}

void SpeechSynthesizer::speak(const wchar_t *prompt) {
	HRESULT hr = pVoice->Speak(createPromptWitthHints(prompt).c_str(), SPF_ASYNC | SPF_PURGEBEFORESPEAK, NULL);
    assert(SUCCEEDED(hr));
    if (SUCCEEDED(hr)) {
        pVoice->WaitUntilDone(INFINITE);
    }
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
}

std::wstring SpeechSynthesizer::speak(const wchar_t *prompt, const wchar_t* path) {
    HRESULT hr = S_OK;
    HRESULT hr2 = S_OK;
    const std::wstring soundFile = std::wstring(path) + L".wav";
    //Set the audio format
    CSpStreamFormat cAudioFmt;
    hr = cAudioFmt.AssignFormat(SPSF_22kHz16BitMono);
    assert(SUCCEEDED(hr));
    //Call SPBindToFile, a SAPI helper method, to bind the audio stream to the file
    if (SUCCEEDED(hr)) {
        CComPtr <ISpStream> cpStream;
        hr = SPBindToFile(
                 soundFile.c_str(),
                 SPFM_CREATE_ALWAYS,
                 &cpStream, &cAudioFmt.FormatId(),
                 cAudioFmt.WaveFormatExPtr(), SPEI_UNDEFINED);
        assert(SUCCEEDED(hr));
        //set the output to cpStream so that the output audio data will be stored in cpStream
        if (SUCCEEDED(hr)) {
            hr = pVoice->SetOutput(cpStream, TRUE);
            assert(SUCCEEDED(hr));
            //Speak the text "hello world" synchronously
            if (SUCCEEDED(hr)) {
				hr = pVoice->Speak(createPromptWitthHints(prompt).c_str(), SPF_DEFAULT, NULL);
                assert(SUCCEEDED(hr));
            }
            cpStream->Close();
            hr2 = pVoice->SetOutput(NULL, TRUE);
            assert(SUCCEEDED(hr2));
        }
        //Release the stream object
        cpStream.Release();
    }
    assert(SUCCEEDED(hr));
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
    if (FAILED(hr2)) {
        throw new COMException(hr2);
    }
    return soundFile;
}

void SpeechSynthesizer::stop() {
    // Purge any ongoing speech, then speak nothing
    HRESULT hr = pVoice->Speak(L"", SPF_ASYNC | SPF_PURGEBEFORESPEAK, NULL);
    assert(SUCCEEDED(hr));
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
}
