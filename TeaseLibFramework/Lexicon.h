#pragma once

#include <sapi.h>

class Lexicon
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

