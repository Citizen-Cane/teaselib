#pragma once

#include "SpHelper.h"

class TEASELIB_FRAMEWORK_EXPORT Language {
public:
	static const wchar_t* Unknown;

	Language(ISpObjectToken * pVoiceToken);
	~Language();

	static LANGID getLangID(ISpObjectToken * pVoiceToken);
	static LANGID getLangID(const wchar_t* locale);
	static std::wstring getLangIDStringWithoutLeadingZeros(const wchar_t* locale);
	static void getName(LANGID langID, wchar_t* sname, int size);
	static void getDisplayName(LANGID langID, wchar_t* displayName, int size);

	LANGID langID;
	TCHAR sname[6];
	TCHAR displayName[MAX_PATH];
};
