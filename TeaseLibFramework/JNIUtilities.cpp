#include "stdafx.h"

#include <jni.h>

#include "JNIUtilities.h"

using namespace std;

std::vector<std::wstring> JNIUtilities::stringArray(JNIEnv* env, jobjectArray jarray) {
    vector<wstring> strings;
    if (jarray) {
        int stringCount = env->GetArrayLength(jarray);
        for (int i = 0; i < stringCount; i++) {
            jstring string = (jstring)env->GetObjectArrayElement(jarray, i);
            const jchar* rawString = env->GetStringChars(string, 0);
            strings.push_back(std::wstring((const wchar_t*)rawString, env->GetStringLength(string)));
            // Don't forget to call `ReleaseStringUTFChars` when you're done.
            env->ReleaseStringChars(string, rawString);
        }
    }
    return strings;
}
