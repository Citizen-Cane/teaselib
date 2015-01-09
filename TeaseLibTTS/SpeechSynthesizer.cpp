#include "stdafx.h"

#include <sapi.h>

#include <assert.h>

#include <sphelper.h>

#include "COMException.h"

#include "SpeechSynthesizer.h"
#include "Voice.h"

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
    }
}


SpeechSynthesizer::~SpeechSynthesizer() {
    pVoice->Release();
    pVoice = NULL;
}

void SpeechSynthesizer::speak(const wchar_t *prompt) {
    HRESULT hr = pVoice->Speak(prompt, SPF_ASYNC | SPF_PURGEBEFORESPEAK, NULL);
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
                hr = pVoice->Speak(prompt, SPF_DEFAULT, NULL);
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
