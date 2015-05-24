#include "stdafx.h"

#include <algorithm>
#include <iostream>
#include <thread>

#include <assert.h>
#include <locale.h>

// sphelper in Windows 8.1 SDK uses deprecated function GetVersionEx,
// so for the time being, we'll use a copy with the annoying stuff commented out or changed
#include "sphelper.h"

#include <COMException.h>

#include "AudioLevelUpdatedEvent.h"
#include "AudioLevelSignalProblemOccuredEvent.h"
#include "SpeechRecognitionStartedEvent.h"
#include "SpeechRecognizedEvent.h"

#include "SpeechRecognizer.h"


SpeechRecognizer::SpeechRecognizer(JNIEnv *env, jobject jthis, jobject jevents, const wchar_t* locale)
    : NativeObject(env, jthis)
    , speechRecognitionThread()
    , locale(locale)
    , hExitEvent(INVALID_HANDLE_VALUE)
    , gjthis(env->NewGlobalRef(jthis))
    , gjevents(env->NewGlobalRef(jevents))
    , threadEnv(NULL) {
    assert(env);
    assert(jthis);
    assert(jevents);
    assert(locale);
    if (jevents == NULL) {
        throw new NativeException(E_POINTER, L"Events");
    }
    if (locale == NULL) {
        throw new NativeException(E_POINTER, L"Locale");
    }
}

SpeechRecognizer::~SpeechRecognizer() {
    if (hExitEvent != INVALID_HANDLE_VALUE) {
        SetEvent(hExitEvent);
    }
    speechRecognitionThread.join();
    env->DeleteGlobalRef(gjthis);
    env->DeleteGlobalRef(gjevents);
}

void SpeechRecognizer::speechRecognitionEventHandlerThread(JNIEnv* threadEnv) {
    this->threadEnv = threadEnv;
    assert(threadEnv);
    recognizerStatus = speechRecognitionInitAudio();
    assert(SUCCEEDED(recognizerStatus));
    if (SUCCEEDED(recognizerStatus)) {
        recognizerStatus = speechRecognitionInitInterests();
        assert(SUCCEEDED(recognizerStatus));
        // Establish a Win32 event to signal when speech events are available
        HANDLE hSpeechNotifyEvent = INVALID_HANDLE_VALUE;
        recognizerStatus = cpContext->SetNotifyWin32Event();
        assert(SUCCEEDED(recognizerStatus));
        if (SUCCEEDED(recognizerStatus)) {
            // Establish a separate win32 event to signal event loop exit
            hExitEvent = CreateEvent(NULL, FALSE, FALSE, NULL);
            HANDLE hSpeechNotifyEvent = cpContext->GetNotifyEventHandle();
            if (INVALID_HANDLE_VALUE == hSpeechNotifyEvent) {
                // Notification handle unsupported
                recognizerStatus = E_NOINTERFACE;
            }
            assert(SUCCEEDED(recognizerStatus));
            if (SUCCEEDED(recognizerStatus)) {
                // Collect the events listened for to pump the speech event loop
                HANDLE rghEvents[] = { hSpeechNotifyEvent, hExitEvent };
                speechRecognitionEventHandlerLoop(rghEvents);
            }
        }
    }
}

HRESULT SpeechRecognizer::speechRecognitionInitContext() {
    //std::wstring id = getLOCALE_ILANGUAGE(langID);
    //if (id.empty())
    //{
    //  throw new NativeException(E_INVALIDARG, L"Unsupported language or region");
    //}
    TCHAR languageID[MAX_PATH];
    int size = GetLocaleInfoEx(locale.c_str(), LOCALE_ILANGUAGE, languageID, MAX_PATH);
    const wchar_t* langIDWithoutTrailingZeros = languageID;
    while (*langIDWithoutTrailingZeros == '0') {
        langIDWithoutTrailingZeros++;
    }
    std::wstring recognizerAttributes = std::wstring(L"language=") + langIDWithoutTrailingZeros;
    // Find the best matching installed recognizer for the language
    CComPtr<ISpObjectToken> cpRecognizerToken;
    HRESULT hr = SpFindBestToken(SPCAT_RECOGNIZERS, recognizerAttributes.c_str(), NULL, &cpRecognizerToken);
    assert(SUCCEEDED(hr));
    if (cpRecognizerToken == NULL) {
        throw new NativeException(FAILED(hr) ? hr : E_INVALIDARG, (std::wstring(L"Unsupported language or region '") + locale +
                                  L"'. Please install the corresponding Windows language pack.").c_str());
    }
    // Create a recognizer and immediately set its state to inactive
    if (SUCCEEDED(hr)) {
        hr = cpRecognizer.CoCreateInstance(CLSID_SpInprocRecognizer);
        assert(SUCCEEDED(hr));
        if (SUCCEEDED(hr)) {
            hr = cpRecognizer->SetRecognizer(cpRecognizerToken);
            assert(SUCCEEDED(hr));
            if (SUCCEEDED(hr)) {
                hr = cpRecognizer->SetRecoState(SPRST_INACTIVE);
                assert(SUCCEEDED(hr));
                // Create a new recognition context from the recognizer
                if (SUCCEEDED(hr)) {
                    hr = cpRecognizer->CreateRecoContext(&cpContext);
                    assert(SUCCEEDED(hr));
                    if (SUCCEEDED(hr)) {
                        HRESULT hr = cpContext->SetContextState(SPCS_DISABLED);
                        assert(SUCCEEDED(hr));
                        if (SUCCEEDED(hr)) {
                            hr = cpRecognizer->SetRecoState(SPRST_ACTIVE);
                        }
                    }
                }
            }
        }
    }
    return hr;
}

HRESULT SpeechRecognizer::speechRecognitionInitAudio() {
    // Initialize an audio object to use the default audio input of the system and set the recognizer to use it
    HRESULT hr = cpAudioIn.CoCreateInstance(CLSID_SpMMAudioIn);
    assert(SUCCEEDED(hr));
    // This will typically use the microphone input
    if (SUCCEEDED(hr)) {
        hr = cpRecognizer->SetInput(cpAudioIn, TRUE);
        assert(SUCCEEDED(hr));
        if (SUCCEEDED(hr)) {
            // Populate a WAVEFORMATEX struct with our desired output audio format information.
            WAVEFORMATEX* pWfexCoMemRetainedAudioFormat = NULL;
            GUID guidRetainedAudioFormat = GUID_NULL;
            hr = SpConvertStreamFormatEnum(SPSF_16kHz16BitMono, &guidRetainedAudioFormat, &pWfexCoMemRetainedAudioFormat);
            assert(SUCCEEDED(hr));
        }
    }
    return hr;
}

HRESULT SpeechRecognizer::speechRecognitionInitInterests() {
    // Subscribe to the speech recognition event and end stream event
    ULONGLONG ullEventInterest =
        SPFEI(SPEI_PHRASE_START) |
        SPFEI(SPEI_HYPOTHESIS) |
        SPFEI(SPEI_FALSE_RECOGNITION) |
        SPFEI(SPEI_INTERFERENCE) |
        SPFEI(SPEI_SR_AUDIO_LEVEL) |
        SPFEI(SPEI_RECOGNITION);
    HRESULT hr = cpContext->SetInterest(ullEventInterest, ullEventInterest);
    assert(SUCCEEDED(hr));
    return hr;
}

void SpeechRecognizer::speechRecognitionEventHandlerLoop(HANDLE* rghEvents) {
    // Speech recognition event loop
    bool interrupted = false;
    while (!interrupted && SUCCEEDED(recognizerStatus)) {
        // Wait for either a speech event or an exit event
        DWORD dwMessage = WaitForMultipleObjects(sp_countof(rghEvents), rghEvents, FALSE, INFINITE);
        switch (dwMessage) {
        // With the WaitForMultipleObjects call above, WAIT_OBJECT_0 is a speech event from hSpeechNotifyEvent
        case WAIT_OBJECT_0: {
            // Sequentially grab the available speech events from the speech event queue
            CSpEvent spevent;
            while (S_OK == spevent.GetFrom(cpContext)) {
                try {
                    switch (spevent.eEventId) {
                    case SPEI_PHRASE_START: {
                        // Start of recognition
                        SpeechRecognitionStartedEvent(threadEnv, gjthis, gjevents, "recognitionStarted").fire();
                        break;
                    }
                    case SPEI_HYPOTHESIS: {
                        // Estimated result - can still be recognized false
                        ISpRecoResult* pResult = spevent.RecoResult();
                        LPWSTR pszCoMemResultText = NULL;
                        recognizerStatus = pResult->GetText(SP_GETWHOLEPHRASE, SP_GETWHOLEPHRASE, false, &pszCoMemResultText, NULL);
                        if (SUCCEEDED(recognizerStatus)) {
                            // TODO No alternate phrases, but may produce the correct text -> create event with a single result object
                            wprintf(L"Hypothesis event received, text=\"%s\"\r\n", pszCoMemResultText);
                            SpeechRecognizedEvent(threadEnv, gjthis, gjevents, "speechDetected").fire(pResult);
                        }
                        if (NULL != pszCoMemResultText) {
                            CoTaskMemFree(pszCoMemResultText);
                        }
                        break;
                    }
                    case SPEI_FALSE_RECOGNITION: {
                        // False recognition
                        ISpRecoResult* pResult = spevent.RecoResult();
                        LPWSTR pszCoMemResultText = NULL;
                        recognizerStatus = pResult->GetText(SP_GETWHOLEPHRASE, SP_GETWHOLEPHRASE, false, &pszCoMemResultText, NULL);
                        if (SUCCEEDED(recognizerStatus)) {
                            wprintf(L"False Recognition event received, text=\"%s\"\r\n", pszCoMemResultText);
                            SpeechRecognizedEvent(threadEnv, gjthis, gjevents, "recognitionRejected").fire(pResult);
                        }
                        if (NULL != pszCoMemResultText) {
                            CoTaskMemFree(pszCoMemResultText);
                        }
                        break;
                    }
                    case SPEI_RECOGNITION: {
                        // Successful recognition
                        ISpRecoResult* pResult = spevent.RecoResult();
                        SpeechRecognizedEvent(threadEnv, gjthis, gjevents, "recognitionCompleted").fire(pResult);
                        break;
                    }
                    case SPEI_TTS_AUDIO_LEVEL: {
                        // Audio level for level meter
                        assert(spevent.lParam == 0);
                        AudioLevelUpdatedEvent(threadEnv, gjthis, gjevents, "audioLevelUpdated").fire(spevent.wParam);
                        break;
                    }
                    case SPEI_INTERFERENCE: {
                        // Audio interference
                        SPINTERFERENCE interference = static_cast<SPINTERFERENCE>(spevent.lParam);
                        assert(spevent.wParam == 0);
                        AudioLevelSignalProblemOccuredEvent(threadEnv, gjthis, gjevents, "audioSignalProblemOccured").fire(interference);
                        break;
                    }
                    case SPEI_RECO_STATE_CHANGE: {
                        // The recognizer changes its state
                        break;
                    }
                    }
                } catch (NativeException *e) {
                    // TODO log in teaselib
                    // JNIException::throwNew(env, e);
                    assert(false);
                    wprintf(L"Native exception 0x%x: %s\n", e->errorCode, e->message.c_str());
                } catch (JNIException *e) {
                    // TODO log in teaselib
                    JNIString message = e->getMessage();
                    assert(false);
                    wprintf(L"JNI exception: %s\n", message.operator LPCWSTR());
                }
            }
            break;
        }
        case WAIT_OBJECT_0 + 1: {
            // Exit event; discontinue the speech loop
            interrupted = true;
            break;
        }
        }
    }
}

void SpeechRecognizer::setChoices(const Choices& choices) {
    if (FAILED(recognizerStatus)) {
        throw new COMException(recognizerStatus);
    }
    if (!cpContext) {
        throw new COMException(E_UNEXPECTED);
    }
    cpGrammar.Release();
    HRESULT hr = cpContext->CreateGrammar(0, &cpGrammar);
    assert(SUCCEEDED(hr));
    if (SUCCEEDED(hr)) {
        // SPSTATEHANDLE hRule;
        // const wchar_t* ruleName = L"Choices";
        // hr = cpGrammar->GetRule(ruleName, 1, SPRAF_TopLevel | SPRAF_Active, TRUE, &hRule);
        assert(SUCCEEDED(hr));
        if (SUCCEEDED(hr)) {
            // Add a rule for each choice, index them so we can return the proper index in the recognitino result
            int n = 0;
            for_each(choices.begin(), choices.end(), [&](const Choices::value_type & choice) {
                const std::wstring ruleName = choice;
                SPSTATEHANDLE hRule;
                hr = cpGrammar->GetRule(ruleName.c_str(), n++, SPRAF_TopLevel | SPRAF_Active, TRUE, &hRule);
                assert(SUCCEEDED(hr));
                if (SUCCEEDED(hr)) {
                    HRESULT hr = cpGrammar->AddWordTransition(hRule, NULL, ruleName.c_str(), L" ", SPWT_LEXICAL, 1, NULL);
                    assert(SUCCEEDED(hr));
                    if (FAILED(hr)) {
                        throw new COMException(hr);
                    }
                }
            });
            hr = cpGrammar->Commit(0);
            assert(SUCCEEDED(hr));
            if (SUCCEEDED(hr)) {
                // activate the grammar since "construction" is finished, and ready for receiving recognitions
                hr = cpGrammar->SetGrammarState(SPGS_ENABLED);
                assert(SUCCEEDED(hr));
                if (SUCCEEDED(hr)) {
                    // Set all top-level rules in the new grammar to the active state.
                    hr = cpGrammar->SetRuleState(NULL, NULL, SPRS_ACTIVE);
                    // hr = cpGrammar->SetRuleState(ruleName, NULL, SPRS_ACTIVE);
                    assert(SUCCEEDED(hr));
                }
            }
        }
    }
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
}

void SpeechRecognizer::setMaxAlternates(const int maxAlternates) {
    if (!cpContext) {
        throw new COMException(E_UNEXPECTED);
    }
    HRESULT hr = cpContext->SetMaxAlternates(maxAlternates);
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
}

void SpeechRecognizer::startRecognition() {
    if (FAILED(recognizerStatus)) {
        throw new COMException(recognizerStatus);
    }
    if (!cpContext) {
        throw new COMException(E_UNEXPECTED);
    }
    HRESULT hr = cpRecognizer->SetRecoState(SPRST_ACTIVE_ALWAYS);
    assert(SUCCEEDED(hr));
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
    hr = cpContext->SetContextState(SPCS_ENABLED);
    assert(SUCCEEDED(hr));
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
}

void SpeechRecognizer::stopRecognition() {
    if (FAILED(recognizerStatus)) {
        throw new COMException(recognizerStatus);
    }
    if (!cpContext) {
        throw new COMException(E_UNEXPECTED);
    }
    HRESULT hr = cpContext->SetContextState(SPCS_DISABLED);
    assert(SUCCEEDED(hr));
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
}
