#pragma once

#include <string>
#include <vector>

#include <jni.h>

#include "NativeObject.h"

class JNIUtilities
{
public:
	static std::vector<std::wstring> stringArray(JNIEnv* env, jobjectArray jarray);
	static jobject jList(JNIEnv* env, const std::vector<NativeObject*>& objects);
};
