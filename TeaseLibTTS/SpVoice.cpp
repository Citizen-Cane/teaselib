#include "stdafx.h"

#include <assert.h>

#include <locale.h>
#include <sapi.h>
#include <sperror.h>

#include <COMException.h>
#include <JNIString.h>
#include <Language.h>

#include "SpVoice.h"

// sphelper in Windows 8.1 SDK uses deprecated function GetVersionEx,
// so for the time being, we'll use a copy with the annoying stuff commented out or changed
#include "sphelper.h"

using namespace std;

wstring getGuid(ISpObjectToken* pVoiceToken) {
    LPWSTR id;
    HRESULT hr = pVoiceToken->GetId(&id);
    if (FAILED(hr)) throw COMException(hr);
    wstring guid = ::PathFindFileName(id);
    CoTaskMemFree(id);
    return guid;
}

SpVoice::SpVoice(JNIEnv* env, jobject ttsImpl, ISpObjectToken* pVoiceToken)
	: Voice(env)
	, pVoiceToken(pVoiceToken)
{
	pVoiceToken->AddRef();

    const std::wstring guid = getGuid(pVoiceToken);
    const Language language(pVoiceToken);

	// Gender
	LPWSTR gender;
	HRESULT hr = SpGetAttribute(pVoiceToken, L"Gender", &gender);
	if (FAILED(hr)) throw COMException(hr);
	jobject jgender = getGenderField(gender);

	// Full name (Vendor, version, Name)
	LPWSTR vendor;
	hr = SpGetAttribute(pVoiceToken, L"Vendor", &vendor);
	if (FAILED(hr)) throw COMException(hr);

	// Name
	LPWSTR voiceName;
	hr = pVoiceToken->GetStringValue(NULL, &voiceName);
	if (FAILED(hr)) throw COMException(hr);

	jobject jvoiceInfo = newVoiceInfo(JNIString(env, vendor), JNIString(env, language.displayName), JNIString(env, voiceName));
	jthis = env->NewGlobalRef(newNativeVoice(ttsImpl, JNIString(env, guid), jgender, JNIString(env, language.sname), jvoiceInfo));
}

SpVoice::~SpVoice() {
    pVoiceToken->Release();
}

SpVoice::operator ISpObjectToken*() const {
    return pVoiceToken;
}

// Why can't M$ do something right and comfortable? - copy from SpHelper, slightly changed to read any attribute
HRESULT SpVoice::SpGetAttribute(_In_ ISpObjectToken * pObjToken, _In_ LPCWSTR pAttributeName, _Outptr_ PWSTR *ppszDescription) {
    if (ppszDescription == nullptr || pObjToken == nullptr) return E_POINTER;

    *ppszDescription = nullptr;
    WCHAR* pRegKeyPath = 0;
    WCHAR* pszTemp = 0;
    HKEY   handle = NULL;
    HRESULT hr = pObjToken->GetId(&pszTemp);
    if (SUCCEEDED(hr)) {
        LONG   lErrorCode = ERROR_SUCCESS;
        pRegKeyPath = wcschr(pszTemp, L'\\');    // Find the first occurance of '\\' in the absolute registry key path
        if (pRegKeyPath) {
            *pRegKeyPath = L'\0';
            pRegKeyPath++;                       // pRegKeyPath now points to the path to the recognizer token under the HKLM or HKCR hive
            TCHAR keyPath[MAX_PATH];
            keyPath[0] = L'\0';
            wcscat_s(keyPath, MAX_PATH, pRegKeyPath);
            // Add "Attributes" to pszTemp
            wcscat_s(keyPath, MAX_PATH, L"\\Attributes");
            *ppszDescription = 0;
            // Open the registry key for read and get the handle
            if (wcsncmp(pszTemp, L"HKEY_LOCAL_MACHINE", MAX_PATH) == 0) {
                lErrorCode = RegOpenKeyExW(HKEY_LOCAL_MACHINE, keyPath, 0, KEY_QUERY_VALUE, &handle);
            } else if (wcsncmp(pszTemp, L"HKEY_CURRENT_USER", MAX_PATH) == 0) {
                lErrorCode = RegOpenKeyExW(HKEY_CURRENT_USER, keyPath, 0, KEY_QUERY_VALUE, &handle);
            } else {
                lErrorCode = ERROR_BAD_ARGUMENTS;
            }
            if (ERROR_SUCCESS == lErrorCode) {
                *ppszDescription = (WCHAR*)CoTaskMemAlloc(MAX_PATH * sizeof(WCHAR));
                DWORD count = MAX_PATH;
                lErrorCode = RegGetValue(handle, NULL, pAttributeName, RRF_RT_REG_SZ, NULL, *ppszDescription, &count);
            }
        } else {
            // pRegKeyPath should never be 0 if we are querying for relative hkey path
            lErrorCode = ERROR_BAD_ARGUMENTS;
        }
        hr = HRESULT_FROM_WIN32(lErrorCode);
    }

    if (handle) RegCloseKey(handle);
    if (pszTemp) CoTaskMemFree(pszTemp);
    if (*ppszDescription == nullptr) *ppszDescription = _wcsdup(L"???");
    return hr;
}
