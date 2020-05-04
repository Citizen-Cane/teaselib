#include "stdafx.h"

#include <assert.h>
#include <locale.h>
#include <sapi.h>

#include "COMException.h"

#include "Language.h"

const wchar_t* Language::Unknown = L"??-??";

Language::Language(ISpObjectToken* pVoiceToken) : langID(getLangID(pVoiceToken)) {
	getName(langID, sname, 6);
	getDisplayName(langID, displayName, MAX_PATH);
}

Language::Language(LANGID langId) : langID(langID) {
	getName(langID, sname, 6);
	getDisplayName(langID, displayName, MAX_PATH);
}

Language::~Language() {
}

LANGID Language::getLangID(ISpObjectToken * pVoiceToken) {
	LANGID langID;
	HRESULT hr = SpGetLanguageFromToken(pVoiceToken, &langID);
	if (FAILED(hr)) throw COMException(hr);
	return langID;
}

LANGID Language::getLangID(const wchar_t* locale) {
	TCHAR languageID[MAX_PATH];
	const int charSize = GetLocaleInfoEx(locale, LOCALE_ILANGUAGE, languageID, MAX_PATH);
	assert(charSize > 0);
	if (charSize == 0) throw COMException(E_INVALIDARG);

	return static_cast<WORD>(wcstol(languageID, nullptr, 16));
}

std::wstring Language::getLangIDStringWithoutLeadingZeros(const wchar_t* locale) {
	TCHAR languageID[MAX_PATH];
	const int charSize = GetLocaleInfoEx(locale, LOCALE_ILANGUAGE, languageID, MAX_PATH);
	assert(charSize > 0);
	if (charSize == 0) throw COMException(E_INVALIDARG);

	const wchar_t* langIDWithoutTrailingZeros = languageID;
	while (*langIDWithoutTrailingZeros == '0') {
		langIDWithoutTrailingZeros++;
	}

	return langIDWithoutTrailingZeros;
}

void Language::getName(LANGID langID, wchar_t * sname, int size) {
	// locale e.g. "en-AU"
	wcscpy_s(sname, size, L"??-??");
	GetLocaleInfo(MAKELCID(langID, 0), LOCALE_SNAME, sname, size);
}

void Language::getDisplayName(LANGID langID, wchar_t * displayName, int size) {
	// locale e.g. "en-AU"
	wcscpy_s(displayName, size, L"Unknown");
	GetLocaleInfo(MAKELCID(langID, 0), LOCALE_SLOCALIZEDDISPLAYNAME, displayName, size);
}
