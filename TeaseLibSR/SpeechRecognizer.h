#pragma once

#include <functional>
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


class SpeechRecognizer : public NativeObject, private COMUser
{
public:
	typedef std::vector<std::wstring> Choices;

	SpeechRecognizer(JNIEnv *env, jobject jthis, const wchar_t* recognizerAttributes);
	virtual ~SpeechRecognizer();
	void startEventHandler(JNIEnv* eventHandlerEnv, jobject jevents, const std::function<void(void)>& signalInitialized);

	void setChoices(const Choices& choices);
	void setChoices(const char* srgs, const size_t length);
	void setMaxAlternates(const int maxAlternates);
	void startRecognition();
	void stopRecognition();
	void emulateRecognition(const wchar_t * emulatedRecognitionResult);

private:

	void initContext();
	HRESULT speechRecognitionInitAudio();
	HRESULT resetGrammar();
	void checkRecogizerStatus();

	CComPtr<ISpGrammarCompiler> grammarCompiler;
	CComPtr<ISpRecognizer> cpRecognizer;
	CComPtr<ISpRecoContext> cpContext;
	CComPtr<ISpAudio> cpAudioIn;
	CComPtr<ISpRecoGrammar> cpGrammar;

	const std::wstring locale;
	LANGID langID;

	class EventHandler : public JObject, private COMUser {
	public:
		EventHandler(JNIEnv* env, jobject jevents, ISpRecoContext * cpContext);
		~EventHandler();
		void processEvents(const std::function<void(void)>& signalInitialized);
		HRESULT recognizerStatus;
	private:
		ISpRecoContext* cpContext;
		HRESULT speechRecognitionInitInterests();
		void eventLoop(HANDLE hSpeechNotifyEvent);
		std::mutex thread;
		const HANDLE hExitEvent;
	};
	EventHandler* eventHandler;
};

