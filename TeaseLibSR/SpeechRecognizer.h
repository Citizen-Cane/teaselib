#pragma once

#include <string>
#include <thread>
#include <vector>

#include <atlbase.h>

#include <sapi.h>
#include <sperror.h>

#include <NativeObject.h>
#include <COMUser.h>


class SpeechRecognizer : public NativeObject, protected COMUser
{
public:
	typedef std::vector<std::wstring> Choices;

	SpeechRecognizer(JNIEnv *env, jobject jthis, jobject jevents, const wchar_t* recognizerAttributes);
	virtual ~SpeechRecognizer();
	HRESULT speechRecognitionInitContext();
	void speechRecognitionEventHandlerThread(JNIEnv* threadEnv);

	void setChoices(const Choices& choices);
	void setMaxAlternates(const int maxAlternates);
	void startRecognition();
	void stopRecognition();

private:
	std::thread speechRecognitionThread;
	HANDLE hExitEvent;
	HRESULT recognizerStatus;

	void speechRecognitionEventHandlerLoop(HANDLE* rghEvents);
	HRESULT speechRecognitionInitAudio();
	HRESULT speechRecognitionInitInterests();

	std::wstring locale;

	CComPtr<ISpRecognizer> cpRecognizer;
	CComPtr<ISpRecoContext> cpContext;
	CComPtr<ISpAudio> cpAudioIn;
	CComPtr<ISpRecoGrammar> cpGrammar;

	jobject gjthis;
	jobject gjevents;
	JNIEnv* threadEnv;
};

