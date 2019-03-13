#pragma once

#include <set>
#include <vector>

#include <Event.h>

struct ISpRecoResult;

class SemanticResults {
public:
	typedef std::set<std::wstring> Names;

	SemanticResults(const SPPHRASEPROPERTY * pProperty);
};

class RuleProperties {
public:
	std::vector<std::wstring> args;
	const std::wstring name;
	const int rule_index;
	const int choice_index;

	RuleProperties(const SPPHRASERULE * rule, const SemanticResults& semanticResults);

private:
	static const std::vector<std::wstring> RuleProperties::split(const std::wstring& string, wchar_t delimiter);
	static const wchar_t* ruleName(const SPPHRASERULE * rule, const SemanticResults& semanticResults);
	int ruleIndex(const SPPHRASERULE * rule) const;
	int choiceIndex(const SPPHRASERULE * rule) const;
};

class SpeechRecognizedEvent : public Event
{
public:
	SpeechRecognizedEvent(JNIEnv *env, jobject sender, jobject jevent, const char* name);
	virtual ~SpeechRecognizedEvent();

	void fire(ISpRecoResult* pResult);
private:

	jobject getResult(ISpRecoResult * pResult, const SPPHRASE * pPhrase, const jclass speechRecognitionResultClass);
	static jobject getRule(JNIEnv *env, ISpRecoResult* pResult, const SPPHRASERULE* rule, const SemanticResults& semanticResults);
};

