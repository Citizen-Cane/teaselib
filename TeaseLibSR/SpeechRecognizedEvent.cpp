#include "stdafx.h"

#include <sstream>
#include <map>
#include <vector>

#include <atlbase.h>
#include <sapi.h>
#include <sperror.h>

#include "COMException.h"
#include "JNIException.h"
#include "JNIString.h"

#include "SpeechRecognizedEvent.h"

using namespace std;

SpeechRecognizedEvent::SpeechRecognizedEvent(JNIEnv *env, jobject sender, jobject jevent, const char* name)
    : Event(env, sender, jevent, name) {
}

SpeechRecognizedEvent::~SpeechRecognizedEvent() {
}

const char* getConfidenceFieldName(signed char confidence) {
	if (confidence == SP_LOW_CONFIDENCE) {
		return "Low";
	} else if (confidence == SP_NORMAL_CONFIDENCE) {
		return "Normal";
	} else if (confidence == SP_HIGH_CONFIDENCE) {
		return "High";
	} else {
		assert(false);
		return  "Low";
	}
}

jobject getConfidenceField(JNIEnv *env, signed char confidence) {
    const char* confidenceFieldName = getConfidenceFieldName(confidence);

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

	const SemanticResults semanticResults(pPhrase->pProperties);

	jobject speechRecognitionResult = env->NewObject(
		speechRecognitionResultClass,
		JNIClass::getMethodID(env, speechRecognitionResultClass, "<init>", "(Ljava/lang/String;Lteaselib/core/speechrecognition/Rule;)V"),
		static_cast<jstring>(JNIString(env, text)),
		getRule(env, pResult, &pPhrase->Rule, semanticResults));
	CoTaskMemFree(text);
	if (env->ExceptionCheck()) throw new JNIException(env);

	return speechRecognitionResult;
}

jobject SpeechRecognizedEvent::getRule(JNIEnv *env, ISpRecoResult* pResult, const SPPHRASERULE* rule, const SemanticResults& semanticResults) {
	wchar_t* text;
	HRESULT hr = pResult->GetText(rule->ulFirstElement, rule->ulCountOfElements, false, &text, NULL);
	if (FAILED(hr)) throw new COMException(hr);

	const RuleName ruleNames(rule, semanticResults);

	jclass ruleClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/Rule");
	jobject jRule = env->NewObject(
		ruleClass,
		JNIClass::getMethodID(env, ruleClass, "<init>",
			"(Ljava/lang/String;Ljava/lang/String;IIIFLteaselib/core/speechrecognition/Confidence;)V"),
		static_cast<jstring>(JNIString(env, ruleNames.name.c_str())),
		static_cast<jstring>(JNIString(env, text)),
		ruleNames.choice_index,
		rule->ulFirstElement,
		rule->ulFirstElement + rule->ulCountOfElements,
		rule->SREngineConfidence,
		getConfidenceField(env, rule->Confidence));
	CoTaskMemFree(text);
	if (env->ExceptionCheck()) throw new JNIException(env);

	for (const SPPHRASERULE* childRule = rule->pFirstChild; childRule != NULL; childRule = childRule->pNextSibling) {
		env->CallVoidMethod(jRule, env->GetMethodID(ruleClass, "add", "(Lteaselib/core/speechrecognition/Rule;)V"), getRule(env, pResult, childRule, semanticResults));
		if (env->ExceptionCheck()) throw new JNIException(env);
	}

	return jRule;
}

SemanticResults::SemanticResults(const SPPHRASEPROPERTY * pProperty) 
: names(semanticResults(pProperty)) {}

SemanticResults::Names SemanticResults::semanticResults(const SPPHRASEPROPERTY * pProperty) {
	Names names;
	names.insert({ RuleName::withoutChoiceIndex(pProperty->pszName) , pProperty->pszName });
	if (pProperty->pFirstChild) {
		Names children = semanticResults(pProperty->pFirstChild);
		names.insert(children.begin(), children.end());
	}
	if (pProperty->pNextSibling) {
		Names children = semanticResults(pProperty->pNextSibling);
		names.insert(children.begin(), children.end());
	}
	return names;
}

RuleName::RuleName(const SPPHRASERULE * rule, const SemanticResults& semanticResults)
: name(ruleName(rule, semanticResults))
, args(split(name, L'_'))
, rule_index(ruleIndex(rule))
, choice_index(choiceIndex(rule)) {
}

std::wstring RuleName::withoutChoiceIndex(const wchar_t * pszName) {
	const wstring name = std::wstring(pszName);
	return name.substr(0, name.find_last_of('_'));
}

const vector<wstring> RuleName::split(const wstring& string, wchar_t delimiter) {
	vector<wstring> tokens;
	if (!string.empty()) {
		const size_t bufferSize = 256;
		wchar_t element[bufferSize];
		wistringstream stream(string);
		do {
			stream.getline(element, bufferSize, delimiter);
			tokens.push_back(wstring(element));
		} while (stream.good());
	}
	return tokens;
}

const wchar_t * RuleName::ruleName(const SPPHRASERULE * rule, const SemanticResults & semanticResults) {
	auto choice = semanticResults.names.find(rule->pszName);
	if (choice != semanticResults.names.end()) {
		return choice->second.c_str();
	} else {
		return rule->pszName;
	}
}

int RuleName::ruleIndex(const SPPHRASERULE * rule) const {
	if (args.size() < 2) {
		return INT_MIN;
	} else {
		return stoi(args.at(1));
	}
}

int RuleName::choiceIndex(const SPPHRASERULE* rule) const {
	// TODO Must be 3 to insert semantic result, but thos breaks simple choices
	// -> change either xml naming or naming of simple choices
	// TODO simple choice sr chrashes -> fix regression
	// if (args.size() < 2) {
	if (args.size() < 3) {
		return INT_MIN;
	} else if (args.size() < 3) {
			return rule->ulId;
	} else {
		return stoi(args.at(2));
	}
}
