#pragma once

#include "SpHelper.h"

class Language {
public:
	static const wchar_t* Language::Unknown;

	Language(ISpObjectToken * pVoiceToken);
	Language(LANGID langId);
	~Language();

	static LANGID getLangID(ISpObjectToken * pVoiceToken);
	static LANGID getLangID(const wchar_t* locale);
	static std::wstring Language::getLangIDStringWithoutLeadingZeros(const wchar_t* locale);
	static void getName(LANGID langID, wchar_t* sname, size_t size);
	static void getDisplayName(LANGID langID, wchar_t* displayName, size_t size);

	LANGID langID;
	TCHAR sname[6];
	TCHAR displayName[MAX_PATH];
};

