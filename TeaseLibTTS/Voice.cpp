#include "stdafx.h"

#include <assert.h>

#include <locale.h>
#include <sapi.h>
#include <sperror.h>

#include <COMException.h>
#include <JNIString.h>
#include <Language.h>

#include "Voice.h"

// sphelper in Windows 8.1 SDK uses deprecated function GetVersionEx,
// so for the time being, we'll use a copy with the annoying stuff commented out or changed
#include "sphelper.h"

jobject getGenderField(JNIEnv *env, const wchar_t* gender) {
    const char* genderFieldName;
    //public enum Gender {
    //  Male, Female, Robot
    //}
    if (_wcsicmp(gender, L"Female") == 0) {
        genderFieldName = "Female";
    } else if (_wcsicmp(gender, L"Male") == 0) {
        genderFieldName = "Male";
    } else {
        genderFieldName = "Robot";
    }
    jclass genderClass = JNIClass::getClass(env, "teaselib/core/texttospeech/Voice$Gender");
    jobject genderValue = env->GetStaticObjectField(
                              genderClass,
                              JNIClass::getStaticFieldID(env, genderClass, genderFieldName, "Lteaselib/core/texttospeech/Voice$Gender;"));
    return genderValue;
}

Voice::Voice(JNIEnv* env, ISpObjectToken* pVoiceToken, jobject ttsImpl)
	: NativeObject(env)
	, pVoiceToken(pVoiceToken) {
	pVoiceToken->AddRef();

	// Guid
	LPWSTR id;
	HRESULT hr = pVoiceToken->GetId(&id);
	guid = ::PathFindFileName(id);
	if (FAILED(hr)) throw new COMException(hr);

	Language language(pVoiceToken);

	// Gender
	LPWSTR gender;
	hr = SpGetAttribute(pVoiceToken, L"Gender", &gender);
	if (FAILED(hr)) throw new COMException(hr);
	jobject jgender = getGenderField(env, gender);
	if (env->ExceptionCheck()) throw new JNIException(env);


	jobject jvoiceInfo; {
		// Full name (Vendor, version, Name)
		LPWSTR vendor;
		hr = SpGetAttribute(pVoiceToken, L"Vendor", &vendor);
		if (FAILED(hr)) throw new COMException(hr);

		// Name
		LPWSTR voiceName;
		hr = pVoiceToken->GetStringValue(NULL, &voiceName);
		if (FAILED(hr)) throw new COMException(hr);

		jclass clazz = env->FindClass("teaselib/core/texttospeech/VoiceInfo");
		if (env->ExceptionCheck()) throw new JNIException(env);
		const char* signature = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";

		jvoiceInfo = env->NewObject(
			clazz,
			JNIClass::getMethodID(env, clazz, "<init>", signature),
			JNIString(env, vendor).operator jstring(),
			JNIString(env, language.displayName).operator jstring(),
			JNIString(env, voiceName).operator jstring());
		if (env->ExceptionCheck()) throw new JNIException(env);
	}

	{
		jclass clazz = env->FindClass("teaselib/core/texttospeech/Voice");
		if (env->ExceptionCheck()) throw new JNIException(env);
		const char* signature = "(JLteaselib/core/texttospeech/TextToSpeechImplementation;Ljava/lang/String;Ljava/lang/String;Lteaselib/core/texttospeech/Voice$Gender;Lteaselib/core/texttospeech/VoiceInfo;)V";

		jthis = env->NewObject(
			clazz,
			JNIClass::getMethodID(env, clazz, "<init>", signature),
			reinterpret_cast<jlong>(this),
			ttsImpl,
			JNIString(env, guid.c_str()).operator jstring(),
			JNIString(env, language.sname).operator jstring(),
			jgender,
			jvoiceInfo);
		if (env->ExceptionCheck()) throw new JNIException(env);
	}
	env->DeleteLocalRef(jgender);
    if (env->ExceptionCheck()) {
        throw new JNIException(env);
    }
}

Voice::~Voice() {
    pVoiceToken->Release();
}

Voice::operator ISpObjectToken*() const {
    return pVoiceToken;
}

// Why can't M$ do something right and comfortable? - copy from SpHelper, slightly changed to read any attribute
HRESULT Voice::SpGetAttribute(_In_ ISpObjectToken * pObjToken, _In_ LPCWSTR pAttributeName, _Outptr_ PWSTR *ppszDescription) {
    if (ppszDescription == NULL || pObjToken == NULL) {
        return E_POINTER;
    }
    *ppszDescription = NULL;
    WCHAR* pRegKeyPath = 0;
    WCHAR* pszTemp = 0;
    HKEY   Handle = NULL;
    HRESULT hr = pObjToken->GetId(&pszTemp);
    if (SUCCEEDED(hr)) {
        LONG   lErrorCode = ERROR_SUCCESS;
        pRegKeyPath = wcschr(pszTemp, L'\\');   // Find the first occurance of '\\' in the absolute registry key path
        if (pRegKeyPath) {
            *pRegKeyPath = L'\0';
            pRegKeyPath++;                         // pRegKeyPath now points to the path to the recognizer token under the HKLM or HKCR hive
            TCHAR keyPath[MAX_PATH];
            keyPath[0] = L'\0';
            wcscat_s(keyPath, MAX_PATH, pRegKeyPath);
            // Add "Attributes" to pszTemp
            wcscat_s(keyPath, MAX_PATH, L"\\Attributes");
            *ppszDescription = 0;
            // Open the registry key for read and get the handle
            if (wcsncmp(pszTemp, L"HKEY_LOCAL_MACHINE", MAX_PATH) == 0) {
                lErrorCode = RegOpenKeyExW(HKEY_LOCAL_MACHINE, keyPath, 0, KEY_QUERY_VALUE, &Handle);
            } else if (wcsncmp(pszTemp, L"HKEY_CURRENT_USER", MAX_PATH) == 0) {
                lErrorCode = RegOpenKeyExW(HKEY_CURRENT_USER, keyPath, 0, KEY_QUERY_VALUE, &Handle);
            } else {
                lErrorCode = ERROR_BAD_ARGUMENTS;
            }
            if (ERROR_SUCCESS == lErrorCode) {
                *ppszDescription = (WCHAR*)CoTaskMemAlloc(MAX_PATH * sizeof(WCHAR));
                DWORD count = MAX_PATH;
                lErrorCode = RegGetValue(Handle, NULL, pAttributeName, RRF_RT_REG_SZ, NULL, *ppszDescription, &count);
            }
        } else {
            // pRegKeyPath should never be 0 if we are querying for relative hkey path
            lErrorCode = ERROR_BAD_ARGUMENTS;
        }
        hr = HRESULT_FROM_WIN32(lErrorCode);
    }
    // Close registry key handle
    if (Handle) {
        RegCloseKey(Handle);
    }
    // Free memory allocated to locals
    if (pszTemp) {
        CoTaskMemFree(pszTemp);
    }
    _ASSERT(FAILED(hr) || *ppszDescription != NULL);
    if (FAILED(hr)) {
        // Free memory allocated above if necessary
        if (*ppszDescription != NULL) {
            CoTaskMemFree(*ppszDescription);
            *ppszDescription = NULL;
        }
        *ppszDescription = (WCHAR*)CoTaskMemAlloc(4);
        wcscpy_s(*ppszDescription, 4, L"???");
        hr = S_OK;
    }
    return hr;
}
