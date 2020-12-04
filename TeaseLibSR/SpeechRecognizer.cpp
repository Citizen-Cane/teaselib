#include "stdafx.h"

#include <algorithm>
#include <iomanip>
#include <iostream>
#include <sstream>
#include <thread>

#include <assert.h>
#include <locale.h>

// sphelper in Windows 8.1 SDK uses deprecated function GetVersionEx,
// so for the time being, we'll use a copy with the annoying stuff commented out or changed
#include "sphelper.h"
#include <atlcom.h>

#include <COMException.h>
#include "UnsupportedLanguageException.h"

#include "AudioLevelUpdatedEvent.h"
#include "AudioLevelSignalProblemOccuredEvent.h"
#include "SpeechRecognitionStartedEvent.h"
#include "SpeechRecognizedEvent.h"
#include "UpdateGrammar.h"

#include "SpeechRecognizer.h"

using namespace std;

SpeechRecognizer::SpeechRecognizer(JNIEnv *env, const wchar_t* locale)
    : NativeObject(env)
    , locale(locale)
	, langID(0x0000)
	, eventHandler(nullptr)
{
    assert(env);
    assert(jthis);
    assert(locale);
    if (locale == NULL) throw NativeException(E_POINTER, L"Locale");

	initContext();
}

SpeechRecognizer::~SpeechRecognizer() {
	// takes up to 5 seconds, and is little rude as long as other rules are active
#ifdef _DEBUG
	HRESULT hr = 
#endif
		cpRecognizer->SetRecoState(SPRST_INACTIVE);
	assert(SUCCEEDED(hr));

	delete eventHandler;
}

void SpeechRecognizer::initContext() {
	TCHAR languageID[MAX_PATH];
	const int size = GetLocaleInfoEx(locale.c_str(), LOCALE_ILANGUAGE, languageID, MAX_PATH);
	assert(size > 0);
	if (size == 0) throw NativeException(E_INVALIDARG, locale.c_str());

	const wchar_t* langIDWithoutTrailingZeros = languageID;
	while (*langIDWithoutTrailingZeros == '0') {
		langIDWithoutTrailingZeros++;
	}
	const std::wstring recognizerAttributes = std::wstring(L"language=") + langIDWithoutTrailingZeros;
	// Find the best matching installed recognizer for the language
	CComPtr<ISpObjectToken> cpRecognizerToken;
	HRESULT hr = SpFindBestToken(SPCAT_RECOGNIZERS, recognizerAttributes.c_str(), NULL, &cpRecognizerToken);
	if (cpRecognizerToken == NULL || hr == SPERR_NOT_FOUND) {
		throw UnsupportedLanguageException(FAILED(hr) ? hr : E_INVALIDARG, (std::wstring(L"Unsupported language or region '") + locale +
			L"'. Please install the corresponding Windows language pack.").c_str());
	} else if (FAILED(hr)) {
		assert(SUCCEEDED(hr));
		throw COMException(hr);
	}

	hr = grammarCompiler.CoCreateInstance(CLSID_SpW3CGrammarCompiler);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);

	// Get the lang id in order to be able reset the grammar
	hr = SpGetLanguageFromToken(cpRecognizerToken, &langID);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
	// Create a recognizer and immediately set its state to inactive
	hr = cpRecognizer.CoCreateInstance(CLSID_SpInprocRecognizer);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
	hr = cpRecognizer->SetRecognizer(cpRecognizerToken);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
	hr = cpRecognizer->SetRecoState(SPRST_INACTIVE);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);

	// Create a new recognition context from the recognizer
	hr = cpRecognizer->CreateRecoContext(&cpContext);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
	hr = cpContext->SetContextState(SPCS_DISABLED);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
	hr = cpRecognizer->SetRecoState(SPRST_ACTIVE);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
	hr = cpContext->CreateGrammar(0, &cpGrammar);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);

	hr = speechRecognitionInitAudio();
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);

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

void SpeechRecognizer::startEventHandler(JNIEnv* eventHandlerEnv, jobject jevents, const std::function<void(void)>& signalInitialized) {
	this->eventHandler = new SpeechRecognizer::EventHandler(eventHandlerEnv, jevents, cpContext);
	eventHandler->processEvents(signalInitialized);
}

void SpeechRecognizer::EventHandler::stopEventLoop()
{
	if (hExitEvent != NULL) {
		unique_lock<mutex> lock(thread, std::defer_lock);
		if (!lock.try_lock()) {
			SetEvent(hExitEvent);
			lock.lock();
		}
		lock.unlock();
		CloseHandle(hExitEvent);
	}
}

SpeechRecognizer::EventHandler::EventHandler(JNIEnv* env, jobject jevents, ISpRecoContext * cpContext)
	: JObject(env, jevents)
	, cpContext(cpContext)
	, hExitEvent(CreateEvent(NULL, FALSE, FALSE, NULL))
	, recognizerStatus(S_OK)
{
	assert(hExitEvent);
	if (hExitEvent == NULL) {
		throw NativeException(E_FAIL, L"Exit Event");
	}
}

SpeechRecognizer::EventHandler::~EventHandler() {
	 assert(jthis == nullptr);
}

void SpeechRecognizer::EventHandler::processEvents(const std::function<void(void)>& signalInitialized) {
	lock_guard<mutex> lock(thread);
	signalInitialized();

	shared_ptr<EventHandler> releaseNativeObjects(this, [](EventHandler* eventHandler) {
		eventHandler->env->DeleteGlobalRef(eventHandler->jthis);
		eventHandler->jthis = nullptr;
	});

	recognizerStatus = speechRecognitionInitInterests();
	assert(SUCCEEDED(recognizerStatus));
	if (SUCCEEDED(recognizerStatus)) {
		// Establish a Win32 event to signal when speech events are available
		HANDLE hSpeechNotifyEvent = INVALID_HANDLE_VALUE;
		recognizerStatus = cpContext->SetNotifyWin32Event();
		assert(SUCCEEDED(recognizerStatus));
		if (SUCCEEDED(recognizerStatus)) {
			// Establish a separate win32 event to signal event loop exit
			hSpeechNotifyEvent = cpContext->GetNotifyEventHandle();
			if (INVALID_HANDLE_VALUE == hSpeechNotifyEvent) {
				// Notification handle unsupported
				recognizerStatus = E_NOINTERFACE;
			}
			assert(SUCCEEDED(recognizerStatus));
			if (SUCCEEDED(recognizerStatus)) {
				eventLoop(hSpeechNotifyEvent);
			}
		}
	}
}

HRESULT SpeechRecognizer::EventHandler::speechRecognitionInitInterests() {
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

void SpeechRecognizer::EventHandler::eventLoop(HANDLE hSpeechNotifyEvent) {
    // Speech recognition event loop
    bool interrupted = false;
    while (!interrupted && SUCCEEDED(recognizerStatus)) {
		// Collect the events listened for to pump the speech event loop
		HANDLE rghEvents[] = { hSpeechNotifyEvent, hExitEvent };
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
							SpeechRecognitionStartedEvent(env, jthis, "recognitionStarted").fire();
							break;
						}
						case SPEI_HYPOTHESIS: {
							// Estimated result - can still be recognized false
							ISpRecoResult* pResult = spevent.RecoResult();
							LPWSTR pszCoMemResultText = NULL;
							recognizerStatus = pResult->GetText(SP_GETWHOLEPHRASE, SP_GETWHOLEPHRASE, false, &pszCoMemResultText, NULL);
							if (SUCCEEDED(recognizerStatus)) {
								SpeechRecognizedEvent(env, jthis, "speechDetected").fire(pResult);
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
								SpeechRecognizedEvent(env, jthis, "recognitionRejected").fire(pResult);
							}
							if (NULL != pszCoMemResultText) {
								CoTaskMemFree(pszCoMemResultText);
							}
							break;
						}
						case SPEI_RECOGNITION: {
							// Successful recognition
							ISpRecoResult* pResult = spevent.RecoResult();
							SpeechRecognizedEvent(env, jthis, "recognitionCompleted").fire(pResult);
							break;
						}
						case SPEI_TTS_AUDIO_LEVEL: {
							// Audio level for level meter
							AudioLevelUpdatedEvent(env, jthis, "audioLevelUpdated").fire(spevent.wParam);
							break;
						}
						case SPEI_INTERFERENCE: {
							// Audio interference
							SPINTERFERENCE interference = static_cast<SPINTERFERENCE>(spevent.lParam);
							AudioLevelSignalProblemOccuredEvent(env, jthis, "audioSignalProblemOccured").fire(interference);
							break;
						}
						case SPEI_RECO_STATE_CHANGE: {
							// The recognizer changes its state
							break;
						}
                    }
				} catch (std::exception& e) {
					cerr << "Uncatched std exception in SpeechRecognizer::EventHandler::eventLoop: " << e.what() << endl;
				} catch (NativeException& e) {
					wcerr << "Uncatched native exception in SpeechRecognizer::EventHandler::eventLoop: " << e.message.c_str() << "(error code=" << e.errorCode << ")" << endl;
				} catch (JNIException& e) {
					JNIStringUTF8 message(env, e.getMessage());
					cerr << "Uncatched JNI exception in SpeechRecognizer::EventHandler::eventLoop: " << message.operator LPCSTR() << endl;
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

	UpdateGrammar code(cpContext, [&choices, this]() {
		HRESULT hr = resetGrammar();
		if (FAILED(hr)) throw COMException(hr);

		// Add a rule for each choice, index them so we can return the proper index in the recognitino result
		int n = 0;
		CComPtr<ISpRecoGrammar>& cpGrammar = this->cpGrammar;
		for_each(choices.begin(), choices.end(), [&cpGrammar, &n](const Choices::value_type & choice) {
			SPSTATEHANDLE hRule;
			std::wstringstream ruleName;
			ruleName << L"r_0_" << n;
			HRESULT hr = cpGrammar->GetRule(ruleName.str().c_str(), n++, SPRAF_TopLevel | SPRAF_Active, TRUE, &hRule);
			assert(SUCCEEDED(hr));
			if (FAILED(hr)) throw COMException(hr);
			hr = cpGrammar->AddWordTransition(hRule, NULL, choice.c_str(), L" ", SPWT_LEXICAL, 1, NULL);
			assert(SUCCEEDED(hr));
			if (FAILED(hr)) throw COMException(hr);
		});

		hr = cpGrammar->Commit(0);
		assert(SUCCEEDED(hr));
		if (FAILED(hr)) throw COMException(hr);

		// Set all top-level rules in the new grammar to the active state
		hr = cpGrammar->SetRuleState(NULL, NULL, SPRS_ACTIVE);
		assert(SUCCEEDED(hr));
		if (FAILED(hr)) throw COMException(hr);
	});
}

// TODO Deprecated - replace - CAtlComModule crashes
CComModule _Module;

class ATL_NO_VTABLE ErrorLog : public CComObjectRoot, public ISpErrorLog {
public:
	ErrorLog()
	{
	}

	DECLARE_NOT_AGGREGATABLE(ErrorLog)
	BEGIN_COM_MAP(ErrorLog)
		COM_INTERFACE_ENTRY(ISpErrorLog)
	END_COM_MAP()

	DECLARE_PROTECT_FINAL_CONSTRUCT()

	HRESULT FinalConstruct()
	{
		return S_OK;
	}

	void FinalRelease()
	{
	}


	HRESULT STDMETHODCALLTYPE AddError(
		/* [in] */ const long lLineNumber,
		/* [in] */ HRESULT hr,
		/* [in] */ LPCWSTR pszDescription,
		/* [in][annotation] */
		_In_opt_  LPCWSTR pszHelpFile,
		/* [in] */ DWORD dwHelpContext) override {
		if (errorAdded) errors << std::endl;
		errors << L"line " << lLineNumber;
		errors << L" hr=0x" << std::uppercase << std::setfill(L'0') << std::setw(8) << std::hex << hr;
		errors << " " << pszDescription;
		errorAdded = true;
		return S_OK;
	}

	bool empty() const { return !errorAdded; }
	operator std::wstring() { return errors.str(); }
private:
	std::wstringstream errors;
	bool errorAdded = false;
};

void SpeechRecognizer::setChoices(CComPtr<IStream>& srgs) {
	checkRecogizerStatus();

	CComPtr<IStream> cfg = SHCreateMemStream(NULL, 999999);
	if (!cfg) throw COMException(E_OUTOFMEMORY);

	CComObject<ErrorLog>* errors;
	HRESULT hr = CComObject<ErrorLog>::CreateInstance(&errors);
	if (FAILED(hr)) throw COMException(hr);
	CComPtr<ErrorLog> pErrors = errors;
	hr = grammarCompiler->CompileStream(srgs, cfg, NULL, NULL, pErrors, 0);
	if (!errors->empty()) throw NativeException(hr, *errors);
	if (FAILED(hr)) throw COMException(hr);

	const LARGE_INTEGER start = { 0,0 };
	ULARGE_INTEGER size = { 0,0 };
	cfg->Seek(start, STREAM_SEEK_CUR, &size);
	cfg->Seek(start, STREAM_SEEK_SET, NULL);

	CComPtr<IStream> buffer;
	hr = ::CreateStreamOnHGlobal(NULL, true, &buffer);
	if (FAILED(hr)) throw COMException(hr);
	hr = IStream_Copy(cfg, buffer, size.LowPart);
	if (FAILED(hr)) throw COMException(hr);
	
	HGLOBAL hGrammar;
	hr = GetHGlobalFromStream(buffer, &hGrammar);
	if (FAILED(hr)) throw COMException(hr);

	hr = cpGrammar->LoadCmdFromMemory((SPBINARYGRAMMAR *)::GlobalLock(hGrammar), SPLO_DYNAMIC /* SPLO_STATIC*/);
	GlobalUnlock(hGrammar);
	if (FAILED(hr)) throw COMException(hr);

	hr = cpGrammar->SetRuleState(NULL, NULL, SPRS_ACTIVE);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
}

void SpeechRecognizer::setMaxAlternates(const int maxAlternates) {
	checkRecogizerStatus();
	
	HRESULT hr = cpContext->SetMaxAlternates(maxAlternates);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
}

void SpeechRecognizer::startRecognition() {
	checkRecogizerStatus();

	HRESULT hr = cpRecognizer->SetRecoState(SPRST_ACTIVE);
    assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);

	hr = cpContext->SetContextState(SPCS_ENABLED);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
}

void SpeechRecognizer::stopRecognition() {
	checkRecogizerStatus();

    HRESULT hr = cpContext->SetContextState(SPCS_DISABLED);
    assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);
}

void SpeechRecognizer::emulateRecognition(const wchar_t * emulatedRecognitionResult) {
	checkRecogizerStatus();

	CComPtr<ISpPhraseBuilder> cpPhrase;
	HRESULT hr = CreatePhraseFromText(emulatedRecognitionResult, &cpPhrase, langID);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);

	hr = cpRecognizer->EmulateRecognition(cpPhrase);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw COMException(hr);

}

void SpeechRecognizer::stopEventLoop()
{
	eventHandler->stopEventLoop();
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
	checkThread();

	if (FAILED(eventHandler->recognizerStatus)) {
		assert(false);
		throw COMException(eventHandler->recognizerStatus);
	}

	if (!cpContext) {
		throw COMException(E_UNEXPECTED);
	}
}
