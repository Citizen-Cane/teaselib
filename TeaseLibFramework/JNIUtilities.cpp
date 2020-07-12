#include "stdafx.h"

#include <algorithm>

#include <jni.h>

#include "JNIClass.h"
#include "JNIException.h"
#include "JNIUtilities.h"
#include "NativeObject.h"

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

jobject JNIUtilities::asList(JNIEnv* env, const std::vector<NativeObject*>& voices) {
	jclass listClass = JNIClass::getClass(env, "java/util/ArrayList");
	jobject jvoiceList = env->NewObject(
		listClass,
		JNIClass::getMethodID(env, listClass, "<init>", "(I)V"),
		voices.size());
	if (env->ExceptionCheck()) throw JNIException(env);

	std::for_each(voices.begin(), voices.end(), [&](const NativeObject* nativeObject) {
		env->CallObjectMethod(
			jvoiceList,
			env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z"), nativeObject->operator jobject());
		if (env->ExceptionCheck()) throw JNIException(env);
		});

	return jvoiceList;
}

jobject JNIUtilities::asSet(JNIEnv* env, const std::set<NativeObject*>& voices) {
	jclass listClass = JNIClass::getClass(env, "java/util/HashSet");
	jobject jvoiceList = env->NewObject(
		listClass,
		JNIClass::getMethodID(env, listClass, "<init>", "(I)V"),
		voices.size());
	if (env->ExceptionCheck()) throw JNIException(env);

	std::for_each(voices.begin(), voices.end(), [&](const NativeObject* nativeObject) {
		env->CallObjectMethod(
			jvoiceList,
			env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z"), nativeObject->operator jobject());
		if (env->ExceptionCheck()) throw JNIException(env);
		});

	return jvoiceList;
}

jobject JNIUtilities::enumValue(JNIEnv* env, const char* enumClass, const char* name)
{
	jclass cls = JNIClass::getClass(env, enumClass);
	jfieldID enumField = JNIClass::getStaticFieldID(env, cls, name, (std::string("L") + enumClass + ";").c_str());
	return env->GetStaticObjectField(cls, enumField);
}
