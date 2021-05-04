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
	const std::vector<int> choice_indices;

	RuleName(const SPPHRASERULE * rule, const SemanticResults& semanticResults);
	static std::wstring withoutChoiceIndex(const wchar_t* pszName);
private:
	static const std::vector<std::wstring> split(const std::wstring& string, wchar_t delimiter);
	static const wchar_t* ruleName(const SPPHRASERULE * rule, const SemanticResults& semanticResults);
	int ruleIndex() const;
	std::vector<int> choiceIndex() const;
};

class SpeechRecognizedEvent : public Event {
public:
	SpeechRecognizedEvent(JNIEnv *env,  jobject jevent, const char* name, jobject jteaselib_sr);
	
	void fire(ISpRecoResult* pResult);
private:
	const jclass confidenceClass;
	jobject getConfidenceField(signed char confidence) const;
	const jclass ruleClass;
	jobject getRule(ISpRecoResult* pResult, const SPPHRASERULE* rule, const SemanticResults& semanticResults) const;
	jobject newRule(
		const wchar_t* name, const wchar_t* text, int rule_index,
		jobject choiceIndices, ULONG fromElement, ULONG toElement,
		float probability, jobject confidence) const;
	jobject newRule(
		const wchar_t* name, const wchar_t* text, int rule_index,
		const std::vector<jobject>& children,
		ULONG fromElement, ULONG toElement, float probability, jobject confidence) const;
	jobject choiceIndices(const RuleName& ruleName) const;

	jobject repair(jobject jrules);
	jobject const jteaselibsr;
};
