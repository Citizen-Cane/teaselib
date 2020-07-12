#pragma once

#include <set>
#include <string>
#include <vector>

#include <jni.h>

#include "NativeObject.h"

class JNIUtilities
{
public:
	static std::vector<std::wstring> stringArray(JNIEnv* env, jobjectArray jarray);
	static jobject asList(JNIEnv* env, const std::vector<NativeObject*>& objects);
	static jobject asSet(JNIEnv* env, const std::set<NativeObject*>& objects);

	static jobject enumValue(JNIEnv* env, const char* enumClass, const char* value);
};
