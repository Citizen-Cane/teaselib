#include "stdafx.h"

#include <algorithm>
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

SpeechRecognizedEvent::SpeechRecognizedEvent(JNIEnv *env,  jobject jevent, const char* name)
    : Event(env, jevent, name)
	, ruleClass(JNIClass::getClass(env, "teaselib/core/speechrecognition/Rule"))
	, confidenceClass(JNIClass::getClass(env, "teaselib/core/speechrecognition/Confidence"))
{}

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

jobject SpeechRecognizedEvent::getConfidenceField(JNIEnv *env, signed char confidence) const {
    const char* confidenceFieldName = getConfidenceFieldName(confidence);
    jobject confidenceValue = env->GetStaticObjectField(
		confidenceClass,
		JNIClass::getStaticFieldID(env, confidenceClass, confidenceFieldName, "Lteaselib/core/speechrecognition/Confidence;"));
	if (env->ExceptionCheck()) throw JNIException(env);

	return confidenceValue;
}


void SpeechRecognizedEvent::fire(ISpRecoResult* pResult) {
	SPPHRASE* pPhrase = nullptr;
	HRESULT hr = pResult->GetPhrase(&pPhrase);
	if (FAILED(hr)) throw COMException(hr);
	// TODO review handling of empty phrase as an result of an unrecognized phrase with emulateRecognition srg xml
	if (pPhrase->Rule.pszName == nullptr) return; //  throw COMException(SPERR_EMPTY_RULE);

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
		if (FAILED(hr)) throw COMException(hr);
	}
	else {
		ulAlternatesCount = 0;
	}

	jobjectArray speechRecognitionResults = nullptr;
    if (ulAlternatesCount > 0) {
		speechRecognitionResults = env->NewObjectArray(ulAlternatesCount, ruleClass, nullptr);
		if (env->ExceptionCheck()) throw JNIException(env);
        for (int i = 0; i < ulAlternatesCount; i++) {
            SPPHRASE* pAlternatePhrase;
            hr = pPhraseAlt[i]->GetPhrase(&pAlternatePhrase);
            if (FAILED(hr)) throw COMException(hr);

			const SemanticResults semanticResults(pAlternatePhrase->pProperties);
			jobject rule = getRule(pResult, &pAlternatePhrase->Rule, semanticResults);
			env->SetObjectArrayElement(speechRecognitionResults, i, rule);
			// TODO resolve memory leak on exception
			CoTaskMemFree(pAlternatePhrase);
            pPhraseAlt[i]->Release();
            if (env->ExceptionCheck()) throw JNIException(env);
        }
    } else {
		const SemanticResults semanticResults(pPhrase->pProperties);
		speechRecognitionResults = env->NewObjectArray(1, ruleClass, getRule(pResult, &pPhrase->Rule, semanticResults));
        if (env->ExceptionCheck()) throw JNIException(env);
	}
	// TODO resolve memory leak on exception
	CoTaskMemFree(pPhrase);

	jclass eventClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/events/SpeechRecognizedEventArgs");
    jobject eventArgs = env->NewObject(
                            eventClass,
                            JNIClass::getMethodID(env, eventClass, "<init>", "([Lteaselib/core/speechrecognition/Rule;)V"),
                            speechRecognitionResults);
    if (env->ExceptionCheck()) throw JNIException(env);

    __super::fire(eventArgs);
}

jobject SpeechRecognizedEvent::getRule(ISpRecoResult* pResult, const SPPHRASERULE* rule, const SemanticResults& semanticResults) const {
	wchar_t* text;
	HRESULT hr = pResult->GetText(rule->ulFirstElement, rule->ulCountOfElements, false, &text, nullptr);
	if (FAILED(hr)) throw COMException(hr);

	const RuleName ruleName(rule, semanticResults);

	jclass hashSetClass = JNIClass::getClass(env, "java/util/HashSet");
	jobject choiceIndices = env->NewObject(hashSetClass, JNIClass::getMethodID(env, hashSetClass, "<init>", "()V"));
	if (env->ExceptionCheck()) throw JNIException(env);
	jmethodID add = JNIClass::getMethodID(env, hashSetClass, "add", "(Ljava/lang/Object;)Z");

	jclass integerClass = JNIClass::getClass(env, "java/lang/Integer");
	jmethodID valueOf = JNIClass::getStaticMethodID(env, integerClass, "valueOf", "(I)Ljava/lang/Integer;");

	std::for_each(ruleName.choice_indices.begin(), ruleName.choice_indices.end(), [&](const int index) {
		jobject jchoiceIndex = env->CallStaticObjectMethod(integerClass, valueOf, index);
		if (env->ExceptionCheck()) throw JNIException(env);
		env->CallBooleanMethod(choiceIndices, add, jchoiceIndex);
		if (env->ExceptionCheck()) throw JNIException(env);
	});

	jobject jRule = env->NewObject(
		ruleClass,
		JNIClass::getMethodID(env, ruleClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;ILjava/util/Set;IIFLteaselib/core/speechrecognition/Confidence;)V"),
		static_cast<jstring>(JNIString(env, ruleName.name.c_str())),
		text ? static_cast<jstring>(JNIString(env, text)) : nullptr,
		ruleName.rule_index,
		choiceIndices,
		rule->ulFirstElement,
		rule->ulFirstElement + rule->ulCountOfElements,
		rule->SREngineConfidence,
		getConfidenceField(env, rule->Confidence));
	if (text) {
		CoTaskMemFree(text);
	}
	if (env->ExceptionCheck()) throw JNIException(env);

	for (const SPPHRASERULE* childRule = rule->pFirstChild; childRule != nullptr; childRule = childRule->pNextSibling) {
		env->CallVoidMethod(
			jRule,
			env->GetMethodID(ruleClass, "add", "(Lteaselib/core/speechrecognition/Rule;)V"),
			getRule(pResult, childRule, semanticResults));
		if (env->ExceptionCheck()) throw JNIException(env);
	}

	return jRule;
}

SemanticResults::SemanticResults(const SPPHRASEPROPERTY * pProperty) 
: names(semanticResults(pProperty)) {}

SemanticResults::Names SemanticResults::semanticResults(const SPPHRASEPROPERTY * pProperty) {
	Names names;
	if (pProperty) {
		names.insert({ RuleName::withoutChoiceIndex(pProperty->pszName) , pProperty->pszName });
		if (pProperty->pFirstChild) {
			Names children = semanticResults(pProperty->pFirstChild);
			names.insert(children.begin(), children.end());
		}
		if (pProperty->pNextSibling) {
			Names children = semanticResults(pProperty->pNextSibling);
			names.insert(children.begin(), children.end());
		}
	}
	return names;
}

RuleName::RuleName(const SPPHRASERULE * rule, const SemanticResults& semanticResults)
: name(ruleName(rule, semanticResults))
, args(split(name, L'_'))
, rule_index(ruleIndex(rule))
, choice_indices(choiceIndex(rule)) {
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
		try {
			return stoi(args[1]);
		} catch (const std::exception& e) {
			return INT_MIN;
		}
	}
}

std::vector<int> RuleName::choiceIndex(const SPPHRASERULE* rule) const {
	if (args.size() < 3) {
		return std::vector<int>({ INT_MIN });
	} else {
		try {
			std::vector<int> choiceIndices;
			std::wstringstream ss(args[2]);

			for (int i; ss >> i;) {
				choiceIndices.push_back(i);
				if (ss.peek() == ',')
					ss.ignore();
			}

			return choiceIndices;
		} catch (const std::exception& e) {
			return std::vector<int>({ INT_MIN });
		}
	}
}
