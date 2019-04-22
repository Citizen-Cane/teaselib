#pragma once

#include <functional>

#include "sphelper.h"

class UpdateGrammar {
public:
	UpdateGrammar(CComPtr<ISpRecoContext> cpContext, const std::function<void()>& code);
	virtual ~UpdateGrammar();
private:
	CComPtr<ISpRecoContext> cpContext;
};

