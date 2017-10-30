#include "stdafx.h"

#include <algorithm>

#include <sapi.h>

#include <assert.h>

#include <sphelper.h>

#include "COMException.h"
#include "Language.h"

#include "SpeechSynthesizer.h"
#include "Voice.h"

using namespace std;

const wchar_t* SPCAT_VOICES_ONECORE = L"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Speech_OneCore\\Voices";
const wchar_t* SPCAT_VOICES_SPEECH_SERVER = L"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Speech Server\\v11.0\\Voices";

const wchar_t* voiceCategories[] = { SPCAT_VOICES_ONECORE, SPCAT_VOICES_SPEECH_SERVER,  SPCAT_VOICES };

// The debug version speaks very fast without a mood hint, in order to be able to distinguish mood hints from neutral
// The release version values are chosen to be barely noticable, they're supposed to be subtile

#ifdef _DEBUG
	int volumeNeutral = 40;
	int volumeReading = volumeNeutral + 60;
	int rateNeutral = 0;
	int rateReading = -5;
#else
	int volumeNeutral = 50;
	int volumeReading = volumeNeutral + 10;
	int rateNeutral = 0;
	int rateReading = -2;
#endif

SpeechSynthesizer::SpeechSynthesizer(JNIEnv *env, jobject ttsImpl)
    : NativeObject(env, ttsImpl), pVoice(nullptr), cancelSpeech(false), jvoices(nullptr) {
    HRESULT hr = CoCreateInstance(CLSID_SpVoice, NULL, CLSCTX_ALL, IID_ISpVoice, (void **)&pVoice);
    assert(SUCCEEDED(hr));
    if (FAILED(hr)) {
        throw new COMException(hr);
    }

}

SpeechSynthesizer::~SpeechSynthesizer() {
    pVoice->Release();
    pVoice = nullptr;
}

jobject SpeechSynthesizer::voiceList() {
	if (!jvoices) {
		std::vector<Voice*> voices;
		for (int i = 0; i < sizeof(voiceCategories) / sizeof(wchar_t*); i++) {
			HRESULT hr = addVoices(voiceCategories[i], voices);
			if (FAILED(hr)) throw new COMException(hr);
		}
		jvoices = env->NewGlobalRef(jvoiceList(voices));
	}

	return jvoices;
}

HRESULT SpeechSynthesizer::addVoices(const wchar_t* pszCatName, std::vector<Voice*>& voices) {
	CComPtr<IEnumSpObjectTokens> cpEnum;
	HRESULT hr = SpEnumTokens(pszCatName, NULL, NULL, &cpEnum);
	const bool succeeded = SUCCEEDED(hr);
	const bool notAvailable = hr == SPERR_NOT_FOUND;
	assert(succeeded || notAvailable);
	if (notAvailable) return S_FALSE;

	if (succeeded) {
		CComPtr<ISpObjectToken> cpVoiceToken;
		if (SUCCEEDED(hr)) {
			ULONG ulCount = 0;
			hr = cpEnum->GetCount(&ulCount);
			if (SUCCEEDED(hr)) {
				while (ulCount--) {
					hr = cpEnum->Next(1, &cpVoiceToken, NULL);
					if (SUCCEEDED(hr)) {
						voices.push_back(new Voice(env, cpVoiceToken, jthis));
						cpVoiceToken.Release();
					}
					else {
						break;
					}
				}
			}
		}
	}

	return hr;
}

jobject SpeechSynthesizer::jvoiceList(const std::vector<Voice*>& voices) {
	jclass listClass = JNIClass::getClass(env, "java/util/ArrayList");
	jobject jvoiceList = env->NewObject(
		listClass,
		JNIClass::getMethodID(env, listClass, "<init>", "(I)V"),
		voices.size());
	if (env->ExceptionCheck()) throw new JNIException(env);

	std::for_each(voices.begin(), voices.end(), [&](const Voice * voice) {
		env->CallObjectMethod(
			jvoiceList,
			env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z"), voice->operator jobject());
		if (env->ExceptionCheck()) throw new JNIException(env);
	});

	return jvoiceList;
}


// Well, I didn't have much luck with the XML tags,
// because I couldn't reset values in the postfix part of the final prompt
// This may be related to the Loquendo voices I've been using during the hints development,
// but nevertheless prooved unreliable while setting values via COM worked. Good luck!
wstring hintsPromptPrefix;
wstring hintsPromptPostfix;

void SpeechSynthesizer::addLexiconEntry(const wchar_t * const  locale, const wchar_t * const  word, const SPPARTOFSPEECH partOfSpeech, const wchar_t * const  pronunciation) {
	LANGID langID = Language::getLangID(locale);
	CComPtr<ISpPhoneConverter> cpPhoneConv;

	HRESULT hr = SpCreatePhoneConverter(langID, NULL, NULL, &cpPhoneConv);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
	
	SPPHONEID wszId[SP_MAX_PRON_LENGTH];
	hr = cpPhoneConv->PhoneToId(pronunciation, wszId);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

	hr = lexicon.pLexicon->AddPronunciation(word, langID, partOfSpeech, wszId);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
}

void SpeechSynthesizer::setVoice(const Voice * voice) {
	if (voice == nullptr) throw new COMException(E_POINTER);

	HRESULT hr = pVoice->SetVoice(*voice);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

	hr = pVoice->SetVolume(static_cast<USHORT>(volumeNeutral));
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
}

void SpeechSynthesizer::applyHints(const vector<wstring>& hints) {
	hintsPromptPrefix.empty();
	hintsPromptPostfix.empty();

	// Changing the volume via xml tags didn't work as expected,
	// because for a fraction of a second the voice sounded at normal volume
	bool volumeChanged = false;
	bool rateChanged = false;
	for_each(hints.begin(), hints.end(), [&](const wstring& hint) {
		HRESULT hr = S_OK;
		if (hint.compare(L"<mood=reading>") == 0) {
			hr = pVoice->SetVolume(static_cast<USHORT>(volumeReading));
			if (SUCCEEDED(hr)) {
				volumeChanged = true;
				hr = pVoice->SetRate(rateReading);
				if (SUCCEEDED(hr)) {
					rateChanged = true;
				}
			}
		}
		if (FAILED(hr)) throw new COMException(hr);	
	});

	// Defaults
	if (!volumeChanged)	{
		HRESULT hr = pVoice->SetVolume(static_cast<USHORT>(volumeNeutral));
		if (FAILED(hr)) throw new COMException(hr);
	}
	if (!rateChanged) {
		HRESULT hr = pVoice->SetRate(rateNeutral);
		if (FAILED(hr)) throw new COMException(hr);
	}
}

wstring createPromptWitthHints(const wchar_t * prompt) {
	return (hintsPromptPrefix + prompt + hintsPromptPostfix);
}

void SpeechSynthesizer::speak(const wchar_t *prompt) {
	cancelSpeech = false;
	HRESULT hr = pVoice->Speak(createPromptWitthHints(prompt).c_str(), SPF_ASYNC | SPF_PURGEBEFORESPEAK | SPF_IS_XML, NULL);
    assert(SUCCEEDED(hr));
    if (SUCCEEDED(hr)) {
		while(pVoice->WaitUntilDone(100) == S_FALSE) {
			if (cancelSpeech) {
				// Purge speech in progress to stop
				hr = pVoice->Speak(L"", SPF_ASYNC | SPF_PURGEBEFORESPEAK, NULL);
				assert(SUCCEEDED(hr));
				cancelSpeech = false;
				break;
			}
		}
    }
    if (FAILED(hr)) {
        throw new COMException(hr);
    }
}

bool hasSuffix(const std::wstring &str, const std::wstring &suffix) {
	return str.size() >= suffix.size() && str.compare(str.size() - suffix.size(), suffix.size(), suffix) == 0;
}

std::wstring SpeechSynthesizer::speak(const wchar_t *prompt, const wchar_t* path) {
    HRESULT hr = S_OK;
    HRESULT hr2 = S_OK;
    const std::wstring soundFile = std::wstring(path) + (hasSuffix(path, L".wav") ? L"" :  L".wav");
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
				hr = pVoice->Speak(createPromptWitthHints(prompt).c_str(), SPF_DEFAULT | SPF_PARSE_SAPI | SPF_IS_XML, NULL);
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
	// This can be called from any thread
	cancelSpeech = true;
}
