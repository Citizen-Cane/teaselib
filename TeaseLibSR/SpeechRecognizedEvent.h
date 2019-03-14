#pragma once

#include <map>
#include <vector>

#include <Event.h>

struct ISpRecoResult;

class SemanticResults {
public:
	typedef std::map<std::wstring, std::wstring> Names;

	SemanticResults(const SPPHRASEPROPERTY * pProperty);
	const Names names;
private:
	static Names semanticResults(const SPPHRASEPROPERTY * pProperty);
};

class RuleName {
public:
	const std::wstring name;
	const std::vector<std::wstring> args;
	const int rule_index;
	const int choice_index;

	RuleName(const SPPHRASERULE * rule, const SemanticResults& semanticResults);
	static std::wstring withoutChoiceIndex(const wchar_t* pszName);
private:
	static const std::vector<std::wstring> split(const std::wstring& string, wchar_t delimiter);
	static const wchar_t* ruleName(const SPPHRASERULE * rule, const SemanticResults& semanticResults);
	int ruleIndex(const SPPHRASERULE * rule) const;
	int choiceIndex(const SPPHRASERULE * rule) const;
};

class SpeechRecognizedEvent : public Event {
public:
	SpeechRecognizedEvent(JNIEnv *env, jobject sender, jobject jevent, const char* name);
	virtual ~SpeechRecognizedEvent();

	void fire(ISpRecoResult* pResult);
private:

	jobject getResult(ISpRecoResult * pResult, const SPPHRASE * pPhrase, const jclass speechRecognitionResultClass);
	static jobject getRule(JNIEnv *env, ISpRecoResult* pResult, const SPPHRASERULE* rule, const SemanticResults& semanticResults);
};
