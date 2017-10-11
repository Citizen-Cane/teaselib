#include "stdafx.h"

#include <assert.h>
#include "sphelper.h"

#include "COMException.h"
#include "Lexicon.h"


Lexicon::Lexicon() : cpToken(nullptr), cpDataKeyAttributes(nullptr), pLexicon(nullptr) {
	createGlobalLexicon();
}

Lexicon::~Lexicon() {
	if (cpToken) {
		cpToken->Release();
		cpToken = nullptr;
	}

	if (cpDataKeyAttributes) {
		cpDataKeyAttributes->Release();
		cpDataKeyAttributes = nullptr;
	}

	if (pLexicon) {
		pLexicon->Release();
		pLexicon = nullptr;
	}
}

void Lexicon::createApplicationLexicon() {
	LANGID langIDneutral = 0x0409;
	HRESULT hr = SpCreateNewTokenEx(
		SPCAT_APPLEXICONS,
		L"TeaseLib_Token_Key",
		&CLSID_SpUnCompressedLexicon,
		L"TeaseLib TTS Lexicon",
		langIDneutral,
		L"lang dependent name",
		&cpToken,
		&cpDataKeyAttributes);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);

	hr = SpCreateObjectFromToken(cpToken, &pLexicon);
	assert(SUCCEEDED(hr));
	if (FAILED(hr)) throw new COMException(hr);
}

/// The global lexicon is persisted between session, which is not what we want at all
void Lexicon::createGlobalLexicon() {
	HRESULT hr = ::CoCreateInstance(CLSID_SpLexicon, NULL, CLSCTX_INPROC_SERVER, IID_ISpLexicon, reinterpret_cast<void**>(&pLexicon));
	assert(SUCCEEDED(hr));
	if (FAILED(hr))	throw new COMException(hr);
}