#include "stdafx.h"
#include "UpdateGrammar.h"



UpdateGrammar::UpdateGrammar(CComPtr<ISpRecoContext> cpContext, std::tr1::function<void()> code)
	: cpContext(cpContext)
{
	cpContext->Pause(NULL);
	code();
}

UpdateGrammar::~UpdateGrammar()
{
	cpContext->Resume(NULL);
}