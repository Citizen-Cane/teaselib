#pragma once

#include <sapi.h>

class TEASELIB_FRAMEWORK_EXPORT Lexicon
{
public:
	Lexicon();
	~Lexicon();

	ISpObjectToken* cpToken;
	ISpDataKey* cpDataKeyAttributes;
	ISpLexicon* pLexicon;
private:
	void createApplicationLexicon();
	void createGlobalLexicon();
};

