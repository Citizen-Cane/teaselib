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
#include "JNIUtilities.h"

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

	vector<jobject> speechRecognitionResults;
    if (ulAlternatesCount > 0) {
        for (unsigned int i = 0; i < ulAlternatesCount; i++) {
            SPPHRASE* pAlternatePhrase;
            hr = pPhraseAlt[i]->GetPhrase(&pAlternatePhrase);
            if (FAILED(hr)) throw COMException(hr);

			const SemanticResults semanticResults(pAlternatePhrase->pProperties);
			jobject rule = getRule(pResult, &pAlternatePhrase->Rule, semanticResults);

			// TODO filter out empty (invalid) rules here to reduce log output in some cases, but breaks EndAmbiguity tests
			// - filtering would work only for distinct choices anyway, so maybe we should just handle this on the java side

			//jobject indices = env->GetObjectField(
			//	rule, env->GetFieldID(ruleClass, "indices", "Ljava/util/Set;"));
			//if (env->ExceptionCheck()) throw JNIException(env);
			//Objects::requireNonNull(L"indices", indices);

			//jclass setClass = JNIClass::getClass(env, "java/util/Set");
			//jboolean isEmpty = env->CallBooleanMethod(indices, env->GetMethodID(setClass, "isEmpty", "()Z"));
			//if (env->ExceptionCheck()) throw JNIException(env);

			//if (!isEmpty) {
			//	speechRecognitionResults.push_back(rule);
			//}

			speechRecognitionResults.push_back(rule);

			// TODO resolve memory leak on exception
			CoTaskMemFree(pAlternatePhrase);
            pPhraseAlt[i]->Release();
            if (env->ExceptionCheck()) throw JNIException(env);
        }
    } else {
		const SemanticResults semanticResults(pPhrase->pProperties);
		speechRecognitionResults.push_back(getRule(pResult, &pPhrase->Rule, semanticResults));
	}
	// TODO resolve memory leak on exception
	CoTaskMemFree(pPhrase);

	jclass eventClass = JNIClass::getClass(env, "teaselib/core/speechrecognition/events/SpeechRecognizedEventArgs");
    jobject eventArgs = env->NewObject(
                            eventClass,
                            JNIClass::getMethodID(env, eventClass, "<init>", "(Ljava/util/List;)V"),
							JNIUtilities::asList(env, speechRecognitionResults));
    if (env->ExceptionCheck()) throw JNIException(env);

    __super::fire(eventArgs);
}

jobject SpeechRecognizedEvent::getRule(ISpRecoResult* pResult, const SPPHRASERULE* rule, const SemanticResults& semanticResults) const {
	jobject jRule;
	wchar_t* text;
	HRESULT hr = pResult->GetText(rule->ulFirstElement, rule->ulCountOfElements, false, &text, nullptr);
	if (FAILED(hr)) {
		jRule = newRule(L"Invalid", nullptr, -1, newSet(), 0, 0, 0.0f, getConfidenceField(env, rule->Confidence));
		if (env->ExceptionCheck()) throw JNIException(env);
	} else {
		std::vector<jobject> children;
		for (const SPPHRASERULE* childRule = rule->pFirstChild; childRule != nullptr; childRule = childRule->pNextSibling) {
			jobject child = getRule(pResult, childRule, semanticResults);
			if (env->ExceptionCheck()) throw JNIException(env);
			
			children.push_back(child);
		}

		const RuleName ruleName(rule, semanticResults);
		if (children.empty()) {
			jRule = newRule(
				ruleName.name.c_str(),
				text,
				ruleName.rule_index,
				choiceIndices(ruleName),
				rule->ulFirstElement,
				rule->ulFirstElement + rule->ulCountOfElements,
				rule->SREngineConfidence,
				getConfidenceField(env, rule->Confidence));
		} else {
			jRule = newRule(
				ruleName.name.c_str(),
				text,
				ruleName.rule_index,
				children,
				rule->ulFirstElement,
				rule->ulFirstElement + rule->ulCountOfElements,
				rule->SREngineConfidence,
				getConfidenceField(env, rule->Confidence));
		}

		if (text) {
			CoTaskMemFree(text);
		}
		if (env->ExceptionCheck()) throw JNIException(env);
	}

	return jRule;
}
jobject SpeechRecognizedEvent::newRule(
	const wchar_t* name, const wchar_t* text, int rule_index,
	jobject choiceIndices,  ULONG fromElement, ULONG toElement,
	float probability, jobject confidence) const
{
	return env->NewObject(
		ruleClass,
		JNIClass::getMethodID(env, ruleClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;ILjava/util/Set;IIFLteaselib/core/speechrecognition/Confidence;)V"),
		static_cast<jstring>(JNIString(env, name)),
		text ? static_cast<jstring>(JNIString(env, text)) : nullptr,
		rule_index,
		choiceIndices,
		fromElement,
		toElement,
		probability,
		confidence);
}

jobject SpeechRecognizedEvent::newRule(
	const wchar_t* name, const wchar_t* text, int rule_index,
	const std::vector<jobject>& children,
	ULONG fromElement, ULONG toElement, float probability, jobject confidence) const 
{
	return env->NewObject(
		ruleClass,
		JNIClass::getMethodID(env, ruleClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;ILjava/util/List;IIFLteaselib/core/speechrecognition/Confidence;)V"),
		static_cast<jstring>(JNIString(env, name)),
		text ? static_cast<jstring>(JNIString(env, text)) : nullptr,
		rule_index,
		JNIUtilities::asList(env, children),
		fromElement,
		toElement,
		probability,
		confidence);
}

jobject SpeechRecognizedEvent::choiceIndices(const RuleName& ruleName) const {
	jobject choiceIndices = newSet();

	jclass hashSetClass = JNIClass::getClass(env, "java/util/HashSet");
	jmethodID add = JNIClass::getMethodID(env, hashSetClass, "add", "(Ljava/lang/Object;)Z");

	jclass integerClass = JNIClass::getClass(env, "java/lang/Integer");
	jmethodID valueOf = JNIClass::getStaticMethodID(env, integerClass, "valueOf", "(I)Ljava/lang/Integer;");

	std::for_each(ruleName.choice_indices.begin(), ruleName.choice_indices.end(), [&](const int index) {
		jobject jchoiceIndex = env->CallStaticObjectMethod(integerClass, valueOf, index);
		if (env->ExceptionCheck()) throw JNIException(env);
		env->CallBooleanMethod(choiceIndices, add, jchoiceIndex);
		if (env->ExceptionCheck()) throw JNIException(env);
		});

	return choiceIndices;
}

jobject SpeechRecognizedEvent::newSet() const {
	jclass hashSetClass = JNIClass::getClass(env, "java/util/HashSet");
	jobject hashSet = env->NewObject(hashSetClass, JNIClass::getMethodID(env, hashSetClass, "<init>", "()V"));
	if (env->ExceptionCheck()) throw JNIException(env);
	return hashSet;
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
, rule_index(ruleIndex())
, choice_indices(choiceIndex()) {
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

int RuleName::ruleIndex() const {
	if (args.size() < 2) {
		return INT_MIN;
	} else {
		try {
			return stoi(args[1]);
		} catch (const std::exception& /*e*/) {
			return INT_MIN;
		}
	}
}

std::vector<int> RuleName::choiceIndex() const {
	if (args.size() < 3) {
		return std::vector<int>();
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
		} catch (const std::exception& /*e*/) {
			return std::vector<int>();
		}
	}
}
