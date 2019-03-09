#include "stdafx.h"

#include <atlbase.h>
#include <sapi.h>
#include <sperror.h>

#include "COMException.h"
#include "JNIException.h"
#include "JNIString.h"

#include "SpeechRecognizedEvent.h"

SpeechRecognizedEvent::SpeechRecognizedEvent(JNIEnv *env, jobject sender, jobject jevent, const char* name)
    : Event(env, sender, jevent, name) {
}

SpeechRecognizedEvent::~SpeechRecognizedEvent() {
}

jobject getConfidenceField(JNIEnv *env, signed char confidence) {
    const char* confidenceFieldName;
    // public enum Confidence {
    //   Low,
    //   Normal,
    //   High
    // };
    if (confidence == SP_LOW_CONFIDENCE) {
        confidenceFieldName = "Low";
    } else if (confidence == SP_NORMAL_CONFIDENCE) {
        confidenceFieldName = "Normal";
    } else if (confidence == SP_HIGH_CONFIDENCE) {
        confidenceFieldName = "High";
    } else {
        assert(false);
        confidenceFieldName = "Low";
    }

	jclass confidenceClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/Confidence");
    jobject confidenceValue = env->GetStaticObjectField(
		confidenceClass,
		JNIClass::getStaticFieldID(env, confidenceClass, confidenceFieldName, "Lteaselib/core/speechrecognition/Confidence;"));
	if (env->ExceptionCheck()) throw new JNIException(env);

	return confidenceValue;
}

void SpeechRecognizedEvent::fire(ISpRecoResult* pResult) {
	SPPHRASE* pPhrase = NULL;
	HRESULT hr = pResult->GetPhrase(&pPhrase);
	if (FAILED(hr)) throw new COMException(hr);
	// TODO review handling of empty phrase as an result of an unrecognized phrase with emulateRecognition srg xml
	if (pPhrase->Rule.pszName == NULL) return; //  throw new COMException(SPERR_EMPTY_RULE);

	const size_t maxAlternates = 256;
    ISpPhraseAlt* pPhraseAlt[maxAlternates];
    ULONG ulAlternatesCount;
	// Get alternates (alternate spellings) for all the elements (words) in the phrase
	if (pPhrase->Rule.ulCountOfElements > 0) {
		hr = pResult->GetAlternates(
			pPhrase->Rule.ulFirstElement,
			pPhrase->Rule.ulCountOfElements,
			maxAlternates,
			pPhraseAlt,
			&ulAlternatesCount);
		if (FAILED(hr)) throw new COMException(hr);
	}
	else {
		ulAlternatesCount = 0;
	}

    jclass speechRecognitionResultClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/SpeechRecognitionResult");
	if (env->ExceptionCheck()) throw new JNIException(env);

	jobjectArray speechRecognitionResults = NULL;
    if (ulAlternatesCount > 0) {
		speechRecognitionResults = env->NewObjectArray(ulAlternatesCount, speechRecognitionResultClass, NULL);
		if (env->ExceptionCheck()) throw new JNIException(env);
        for (int i = 0; i < ulAlternatesCount; i++) {
            SPPHRASE* pAlternatePhrase;
            hr = pPhraseAlt[i]->GetPhrase(&pAlternatePhrase);
            if (FAILED(hr)) throw new COMException(hr);

			jobject speechRecognitionResult = getResult(pResult, pAlternatePhrase, speechRecognitionResultClass);
			CoTaskMemFree(pAlternatePhrase);
            pPhraseAlt[i]->Release();
            if (env->ExceptionCheck()) throw new JNIException(env);

			env->SetObjectArrayElement(speechRecognitionResults, i, speechRecognitionResult);
            if (env->ExceptionCheck()) throw new JNIException(env);
        }
    } else {
		// Just use the text from the result - if there is one - from the result, for speechDetected or falseRecognition
        speechRecognitionResults = env->NewObjectArray(1, speechRecognitionResultClass, getResult(pResult, pPhrase, speechRecognitionResultClass));
        if (env->ExceptionCheck()) throw new JNIException(env);
	}
	// TODO resolve memory leak on exception
	CoTaskMemFree(pPhrase);

	jclass eventClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/events/SpeechRecognizedEventArgs");
    jobject eventArgs = env->NewObject(
                            eventClass,
                            JNIClass::getMethodID(env, eventClass, "<init>", "([Lteaselib/core/speechrecognition/SpeechRecognitionResult;)V"),
                            speechRecognitionResults);
    if (env->ExceptionCheck()) throw new JNIException(env);

    __super::fire(eventArgs);
}

jobject SpeechRecognizedEvent::getResult(ISpRecoResult* pResult, const SPPHRASE* pPhrase, const jclass speechRecognitionResultClass) {
	wchar_t* text;
	HRESULT hr = pResult->GetText(SP_GETWHOLEPHRASE, SP_GETWHOLEPHRASE, false, &text, NULL);
	if (FAILED(hr)) throw new COMException(hr);

	jobject speechRecognitionResult = env->NewObject(
		speechRecognitionResultClass,
		JNIClass::getMethodID(env, speechRecognitionResultClass, "<init>", "(Ljava/lang/String;Lteaselib/core/speechrecognition/Rule;)V"),
		static_cast<jstring>(JNIString(env, text)),
		getRule(env, pResult, &pPhrase->Rule));
	CoTaskMemFree(text);
	if (env->ExceptionCheck()) throw new JNIException(env);

	return speechRecognitionResult;
}

jobject SpeechRecognizedEvent::getRule(JNIEnv *env, ISpRecoResult* pResult, const SPPHRASERULE* rule) {
	wchar_t* text;
	HRESULT hr = pResult->GetText(rule->ulFirstElement, rule->ulCountOfElements, false, &text, NULL);
	if (FAILED(hr)) throw new COMException(hr);

	jclass ruleClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/Rule");
	jobject jRule = env->NewObject(
		ruleClass,
		JNIClass::getMethodID(env, ruleClass, "<init>",
			"(Ljava/lang/String;Ljava/lang/String;IIIFLteaselib/core/speechrecognition/Confidence;)V"),
		static_cast<jstring>(JNIString(env, rule->pszName)),
		static_cast<jstring>(JNIString(env, text)),
		rule->ulId,
		rule->ulFirstElement,
		rule->ulFirstElement + rule->ulCountOfElements,
		rule->SREngineConfidence,
		getConfidenceField(env, rule->Confidence));
	CoTaskMemFree(text);
	if (env->ExceptionCheck()) throw new JNIException(env);

	for (const SPPHRASERULE* childRule = rule->pFirstChild; childRule != NULL; childRule = childRule->pNextSibling) {
		env->CallVoidMethod(jRule, env->GetMethodID(ruleClass, "add", "(Lteaselib/core/speechrecognition/Rule;)V"), getRule(env, pResult, childRule));
		if (env->ExceptionCheck()) throw new JNIException(env);
	}

	return jRule;
}