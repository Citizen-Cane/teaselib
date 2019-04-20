#pragma once

#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include <atlbase.h>

#include <sapi.h>
#include <sapiddk.h>
#include <sperror.h>

#include <NativeObject.h>
#include <COMUser.h>


class SpeechRecognizer : public NativeObject, protected COMUser
{
public:
	typedef std::vector<std::wstring> Choices;

	SpeechRecognizer(JNIEnv *env, jobject jthis, jobject jevents, const wchar_t* recognizerAttributes);
	virtual ~SpeechRecognizer();
	void speechRecognitionInitContext();
	void speechRecognitionEventHandlerThread(JNIEnv* threadEnv);

	void setChoices(const Choices& choices);
	
	void setChoices(const char* srgs, const size_t length);
	void setMaxAlternates(const int maxAlternates);
	void startRecognition();
	void stopRecognition();
	void emulateRecognition(const wchar_t const * emulatedRecognitionResult);
private:
	void speechRecognitionEventHandlerLoop(HANDLE hSpeechNotifyEvent, HANDLE hExitEvent);
	HRESULT speechRecognitionInitAudio();
	HRESULT speechRecognitionInitInterests();
	HRESULT resetGrammar();
	void checkRecogizerStatus();

	CComPtr<ISpGrammarCompiler> grammarCompiler;
	CComPtr<ISpRecognizer> cpRecognizer;
	CComPtr<ISpRecoContext> cpContext;
	CComPtr<ISpAudio> cpAudioIn;
	CComPtr<ISpRecoGrammar> cpGrammar;

	const std::wstring locale;
	LANGID langID;
	const HANDLE hExitEvent;
	jobject jevents;
	JNIEnv* threadEnv;

	std::mutex eventHandlerThread;
	HRESULT recognizerStatus;
};

