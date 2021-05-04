#pragma once

#include <functional>
#include <set>
#include <string>
#include <vector>

#include <jni.h>

#include "NativeObject.h"

class TEASELIB_FRAMEWORK_EXPORT JNIUtilities
{
public:
    static std::vector<std::string> stringArray(JNIEnv* env, jobjectArray jarray);
    static std::vector<std::wstring> wstringArray(JNIEnv* env, jobjectArray jarray);

	static jobject newList(JNIEnv* env, size_t capacity);
    static jobject newNativeObjectList(JNIEnv* env, size_t capacity);
    static jobject asList(JNIEnv* env, const std::vector<std::string>& elements);
	static jobject asList(JNIEnv* env, const std::vector<NativeObject*>& elements);
	static jobject asList(JNIEnv* env, const std::vector<jobject>& elements);

	static jobject newSet(JNIEnv* env);
	static jobject asSet(JNIEnv* env, const std::set<NativeObject*>& elements);

	static jobject enumValue(JNIEnv* env, const char* enumClass, const char* value);

    template<typename T> static std::vector<T> list(JNIEnv* env, jobject jcollection, const std::function<T(jobject)> element) {

        jclass collectionClass = JNIClass::getClass(env, jcollection);
        jobject iterator = env->CallObjectMethod(jcollection, env->GetMethodID(collectionClass, "iterator", "()Ljava/util/Iterator;"));
        jclass iteratorClass = env->FindClass("Ljava/util/Iterator;");
        if (env->ExceptionCheck()) {
            throw JNIException(env);
        }

        std::vector<T> elements;
        while (env->CallBooleanMethod(iterator, env->GetMethodID(iteratorClass, "hasNext", "()Z"))) {
            jobject jelement = env->CallObjectMethod(iterator, env->GetMethodID(iteratorClass, "next", "()Ljava/lang/Object;"));
            if (env->ExceptionCheck()) {
                throw JNIException(env);
            }
            elements.push_back(element(jelement));
        }

        return elements;
    };

    static std::vector<std::string> strings(JNIEnv* env, jobject jcollection);
    static std::vector<std::wstring> wstrings(JNIEnv* env, jobject jcollection);
    static std::vector<jobjectArray> objectArrays(JNIEnv* env, jobject jcollection);
};
