#pragma once

#include <string>
#include <vector>

#include <jni.h>

class JNIUtilities
{
public:
	static std::vector<std::wstring> stringArray(JNIEnv* env, jobjectArray jarray);
};

