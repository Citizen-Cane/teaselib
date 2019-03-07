#include "stdafx.h"

#include <algorithm>
#include <iostream>
#include <sstream>
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
#include "UpdateGrammar.h"

#include "SpeechRecognizer.h"


SpeechRecognizer::SpeechRecognizer(JNIEnv *env, jobject jthis, jobject jevents, const wchar_t* locale)
    : NativeObject(env, jthis)
    , speechRecognitionThread()
    , locale(locale)
	, langID(0x0000)
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

	// assert(false);
	// TODO remember thread object or thread id
    //speechRecognitionThread.join();

    env->DeleteGlobalRef(gjthis);
    env->DeleteGlobalRef(gjevents);
}

void SpeechRecognizer::speechRecognitionEventHandlerThread(JNIEnv* threadEnv) {
    this->threadEnv = threadEnv;
    assert(threadEnv);
//	speechRecognitionThread = std::this_thread::();
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
            hSpeechNotifyEvent = cpContext->GetNotifyEventHandle();
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

void SpeechRecognizer::speechRecognitionInitContext() {
    TCHAR languageID[MAX_PATH];
    const int size = GetLocaleInfoEx(locale.c_str(), LOCALE_ILANGUAGE, languageID, MAX_PATH);
	assert(size > 0);
	if (size == 0) throw new NativeException(E_INVALIDARG, locale.c_str());

    const wchar_t* langIDWithoutTrailingZeros = languageID;
    while (*langIDWithoutTrailingZeros == '0') {
        langIDWithoutTrailingZeros++;
    }
    const std::wstring recognizerAttributes = std::wstring(L"language=") + langIDWithoutTrailingZeros;
    // Find the best matching installed recognizer for the language
    CComPtr<ISpObjectToken> cpRecognizerToken;
    HRESULT hr = SpFindBestToken(SPCAT_RECOGNIZERS, recognizerAttributes.c_str(), NULL, &cpRecognizerToken);
    assert(SUCCEEDED(hr));
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
	if (cpRecognizerToken == NULL) {
        throw new NativeException(FAILED(hr) ? hr : E_INVALIDARG, (std::wstring(L"Unsupported language or region '") + locale +
                                  L"'. Please install the corresponding Windows language pack.").c_str());
    }

	hr = grammarCompiler.CoCreateInstance(CLSID_SpW3CGrammarCompiler);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

	// Get the lang id in order to be able reset the grammar
	hr = SpGetLanguageFromToken(cpRecognizerToken, &langID);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
	// Create a recognizer and immediately set its state to inactive
	hr = cpRecognizer.CoCreateInstance(CLSID_SpInprocRecognizer);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
	hr = cpRecognizer->SetRecognizer(cpRecognizerToken);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
	hr = cpRecognizer->SetRecoState(SPRST_INACTIVE);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

	// Create a new recognition context from the recognizer
	hr = cpRecognizer->CreateRecoContext(&cpContext);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
	hr = cpContext->SetContextState(SPCS_DISABLED);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
	hr = cpRecognizer->SetRecoState(SPRST_ACTIVE);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
	hr = cpContext->CreateGrammar(0, &cpGrammar);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
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
                            // wprintf(L"Hypothesis event received, text=\"%s\"\r\n", pszCoMemResultText);
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
                            // wprintf(L"False Recognition event received, text=\"%s\"\r\n", pszCoMemResultText);
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
                    // TODO log in teaselib via JNIException::throwNew(env, e) and restart on java side
                    assert(false);
                    wprintf(L"Native exception 0x%x: %s\n", e->errorCode, e->message.c_str());
                } catch (JNIException *e) {
                    // TODO log in teaselib via JNIException::throwNew(env, e) and restart on java side
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
	checkRecogizerStatus();

	UpdateGrammar(cpContext, [&choices, this]() {
		HRESULT hr = resetGrammar();
		if (FAILED(hr)) throw new COMException(hr);

		// Add a rule for each choice, index them so we can return the proper index in the recognitino result
		int n = 0;
		CComPtr<ISpRecoGrammar>& cpGrammar = this->cpGrammar;
		for_each(choices.begin(), choices.end(), [&cpGrammar, &n](const Choices::value_type & choice) {
			const std::wstring ruleName = choice;
			SPSTATEHANDLE hRule;
			HRESULT hr = cpGrammar->GetRule(ruleName.c_str(), n++, SPRAF_TopLevel | SPRAF_Active, TRUE, &hRule);
			assert(SUCCEEDED(hr));
			if (FAILED(hr)) throw new COMException(hr);
			hr = cpGrammar->AddWordTransition(hRule, NULL, ruleName.c_str(), L" ", SPWT_LEXICAL, 1, NULL);
			assert(SUCCEEDED(hr));
			if (FAILED(hr)) throw new COMException(hr);
		});

		hr = cpGrammar->Commit(0);
		assert(SUCCEEDED(hr));
		if (FAILED(hr)) throw new COMException(hr);

		// Set all top-level rules in the new grammar to the active state
		hr = cpGrammar->SetRuleState(NULL, NULL, SPRS_ACTIVE);
		assert(SUCCEEDED(hr));
		if (FAILED(hr)) throw new COMException(hr);
	});
}

class ErrorLog : public ISpErrorLog , CComPtr<ISpErrorLog> {
public:
	std::wstringstream errors;
	HRESULT STDMETHODCALLTYPE AddError(
		/* [in] */ const long lLineNumber,
		/* [in] */ HRESULT hr,
		/* [in] */ LPCWSTR pszDescription,
		/* [in][annotation] */
		_In_opt_  LPCWSTR pszHelpFile,
		/* [in] */ DWORD dwHelpContext) {
		errors << L"line=" << lLineNumber << L" hr=" << hr << " " << pszDescription << std::endl;
	}
};

void SpeechRecognizer::setChoices(const char* srgs, const size_t length) {
	checkRecogizerStatus();

	CComPtr<IStream> xml = SHCreateMemStream(reinterpret_cast<const BYTE*>(srgs), length);
	if (!xml) throw new COMException(E_OUTOFMEMORY);
	CComPtr<IStream> cfg = SHCreateMemStream(NULL, 65536);
	if (!cfg) throw new COMException(E_OUTOFMEMORY);

	// TODO Provide IUnknown impl via ATL
	ErrorLog errors;
	HRESULT hr = grammarCompiler->CompileStream(xml, cfg, NULL, NULL, &errors, 0);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

	const LARGE_INTEGER start = { 0,0 };
	ULARGE_INTEGER size = { 0,0 };
	cfg->Seek(start, STREAM_SEEK_CUR, &size);
	cfg->Seek(start, STREAM_SEEK_SET, NULL);

	CComPtr<IStream> buffer;
	hr = ::CreateStreamOnHGlobal(NULL, true, &buffer);
	if (FAILED(hr)) throw new COMException(hr);
	hr = IStream_Copy(cfg, buffer, size.LowPart);
	if (FAILED(hr)) throw new COMException(hr);
	
	HGLOBAL hGrammar;
	hr = GetHGlobalFromStream(buffer, &hGrammar);
	if (FAILED(hr)) throw new COMException(hr);

	hr = cpGrammar->LoadCmdFromMemory((SPBINARYGRAMMAR *)::GlobalLock(hGrammar), SPLO_DYNAMIC /* SPLO_STATIC*/);
	GlobalUnlock(hGrammar);
	if (FAILED(hr)) throw new COMException(hr);

	hr = cpGrammar->SetRuleState(NULL, NULL, SPRS_ACTIVE);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
}

void SpeechRecognizer::setMaxAlternates(const int maxAlternates) {
	checkRecogizerStatus();
	
	HRESULT hr = cpContext->SetMaxAlternates(maxAlternates);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
}

void SpeechRecognizer::startRecognition() {
	checkRecogizerStatus();

	HRESULT hr = cpRecognizer->SetRecoState(SPRST_ACTIVE_ALWAYS);
    assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

	hr = cpContext->SetContextState(SPCS_ENABLED);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
}

void SpeechRecognizer::stopRecognition() {
	checkRecogizerStatus();

    HRESULT hr = cpContext->SetContextState(SPCS_DISABLED);
    assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

	hr = cpRecognizer->SetRecoState(SPRST_INACTIVE_WITH_PURGE);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
}

void SpeechRecognizer::emulateRecognition(const wchar_t const * emulatedRecognitionResult) {
	checkRecogizerStatus();

	CComPtr<ISpPhraseBuilder> cpPhrase;
	HRESULT hr = CreatePhraseFromText(emulatedRecognitionResult, &cpPhrase, langID);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

	hr = cpRecognizer->EmulateRecognition(cpPhrase);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

}

HRESULT SpeechRecognizer::resetGrammar() {
	// Surprisingly just releasing the grammar may take a lot of time,
	// but the grammar can also be reset, using the langID of the recognizer token
	assert(cpGrammar!= NULL);
	HRESULT hr = cpGrammar->ResetGrammar(langID);
	assert(SUCCEEDED(hr));
	return hr;
}

void SpeechRecognizer::checkRecogizerStatus() {
	assert(SUCCEEDED(recognizerStatus));
	if (FAILED(recognizerStatus)) throw new COMException(recognizerStatus);

	if (!cpContext) {
		throw new COMException(E_UNEXPECTED);
	}
}
