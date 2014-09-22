#include "stdafx.h"

#include <atlbase.h>
#include <sapi.h>

#include "COMException.h"
#include "JNIException.h"
#include "JNIString.h"

#include "SpeechRecognizedEvent.h"

SpeechRecognizedEvent::SpeechRecognizedEvent(JNIEnv *env, jobject sender, jobject jevent, const char* name)
: Event(env, sender, jevent, name)
{
}

SpeechRecognizedEvent::~SpeechRecognizedEvent()
{
}

jobject getConfidenceField(JNIEnv *env, signed char confidence)
{
	const char* confidenceFieldName;
	//public enum Confidence
	//{
	//	Low,
	//	Normal,
	//	High
	//};
	if (confidence == SP_LOW_CONFIDENCE)
	{
		confidenceFieldName = "Low";
	}
	else if (confidence == SP_NORMAL_CONFIDENCE)
	{
		confidenceFieldName = "Normal";
	}
	else if (confidence == SP_HIGH_CONFIDENCE)
	{
		confidenceFieldName = "High";
	}
	else
	{
		assert(false);
		confidenceFieldName = "Low";
	}
	jclass confidenceClass = JNIClass::getClass(env, "teaselib/speechrecognition/SpeechRecognitionResult$Confidence");
	jobject confidenceValue = env->GetStaticObjectField(
		confidenceClass,
		JNIClass::getStaticFieldID(env, confidenceClass, confidenceFieldName, "Lteaselib/speechrecognition/SpeechRecognitionResult$Confidence;"));
	return confidenceValue;
}

void SpeechRecognizedEvent::fire(ISpRecoResult* pResult)
{
	SPPHRASE* pPhrase;
	HRESULT hr = pResult->GetPhrase(&pPhrase);
	if (FAILED(hr)) throw new COMException(hr);
	const size_t maxAlternates = 256;
	ISpPhraseAlt* pPhraseAlt[maxAlternates];
	ULONG ulCount;
	if (pPhrase->Rule.ulCountOfElements > 0)
	{
		// get the top MY_MAX_ALTERNATES alternates to the entire recognized phrase
		hr = pResult->GetAlternates(
			pPhrase->Rule.ulFirstElement,
			pPhrase->Rule.ulCountOfElements,
			maxAlternates,
			pPhraseAlt,
			&ulCount);
	}
	else
	{
		ulCount = 0;
	}
	::CoTaskMemFree(pPhrase);
	if (FAILED(hr)) throw new COMException(hr);
	jclass speechRecognitionResultClass = JNIClass::getClass(env, "teaselib/speechrecognition/SpeechRecognitionResult");
	// Can allocate array only if there is at least one alternate phrase
	jobjectArray speechRecognitionResults = ulCount > 0 
		? env->NewObjectArray(ulCount, speechRecognitionResultClass, NULL)
		: NULL;
	if (env->ExceptionCheck()) throw new JNIException(env);
	if (speechRecognitionResults)
	{
		for (int i = 0; i < ulCount; i++)
		{
			wchar_t* text;
			hr = pPhraseAlt[i]->GetText(SP_GETWHOLEPHRASE, SP_GETWHOLEPHRASE, false, &text, NULL);
			if (FAILED(hr)) throw new COMException(hr);
			SPPHRASE* pAlternatePhrase;
			HRESULT hr = pResult->GetPhrase(&pAlternatePhrase);
			if (FAILED(hr)) throw new COMException(hr);
			jint index = pAlternatePhrase->Rule.ulId;
			jobject confidenceValue = getConfidenceField(env, pAlternatePhrase->Rule.Confidence);
			jobject speechRecognitionResult = env->NewObject(
				speechRecognitionResultClass,
				JNIClass::getMethodID(env, speechRecognitionResultClass, "<init>", "(ILjava/lang/String;DLteaselib/speechrecognition/SpeechRecognitionResult$Confidence;)V"),
				index,
				JNIString(env, text).operator jstring(),
				pAlternatePhrase->Rule.SREngineConfidence,
				confidenceValue);
			CoTaskMemFree(text);
			pPhraseAlt[i]->Release();
			if (env->ExceptionCheck()) throw new JNIException(env);
			env->SetObjectArrayElement(speechRecognitionResults, i, speechRecognitionResult);
			if (env->ExceptionCheck()) throw new JNIException(env);
		}
	}
	else
	{
		// Just use the text from the result - if there is one - from the result, for speechDetected or falseRecognition
		wchar_t* text;
		hr = pResult->GetText(SP_GETWHOLEPHRASE, SP_GETWHOLEPHRASE, false, &text, NULL);
		if (text && SUCCEEDED(hr))
		{
			const bool knownPhrase = pPhrase->cbSize > 0;
			const jint index = knownPhrase ? pPhrase->Rule.ulId : -1;
			const float SREngineConfidence = knownPhrase ? pPhrase->Rule.SREngineConfidence : 0.25;
			const unsigned char confidence = knownPhrase ? pPhrase->Rule.Confidence : SP_LOW_CONFIDENCE;
			jobject speechRecognitionResult = env->NewObject(
				speechRecognitionResultClass,
				JNIClass::getMethodID(env, speechRecognitionResultClass, "<init>", "(ILjava/lang/String;DLteaselib/speechrecognition/SpeechRecognitionResult$Confidence;)V"),
				index,
				JNIString(env, text).operator jstring(),
				SREngineConfidence,
				getConfidenceField(env, confidence));
			CoTaskMemFree(text);
			if (env->ExceptionCheck()) throw new JNIException(env);
			speechRecognitionResults = env->NewObjectArray(1, speechRecognitionResultClass, speechRecognitionResult);
			if (env->ExceptionCheck()) throw new JNIException(env);
		}
		else if (FAILED(hr))
		{
			throw new COMException(hr);
		}
	}
	// Fire the event, pass choices or null array
	jclass eventClass = JNIClass::getClass(env, "teaselib/speechrecognition/events/SpeechRecognizedEventArgs");
	jobject eventArgs = env->NewObject(
		eventClass,
		JNIClass::getMethodID(env, eventClass, "<init>", "([Lteaselib/speechrecognition/SpeechRecognitionResult;)V"),
		speechRecognitionResults);
	if (env->ExceptionCheck()) throw new JNIException(env);
	__super::fire(eventArgs);
}
