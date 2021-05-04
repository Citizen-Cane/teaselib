#include "stdafx.h"

#include <algorithm>
#include <string>

#include <jni.h>

#include "JNIClass.h"
#include "JNIException.h"
#include "JNIString.h"
#include "JNIUtilities.h"
#include "NativeObject.h"

using namespace std;

vector<string> JNIUtilities::stringArray(JNIEnv* env, jobjectArray jarray)
{
	vector<string> elements;
	if (jarray) {
		int stringCount = env->GetArrayLength(jarray);
		for (int i = 0; i < stringCount; i++) {
			JNIStringUTF8 element(env, (jstring) env->GetObjectArrayElement(jarray, i));
			elements.push_back(element.c_str());
		}
	}
	return elements;
}

vector<wstring> JNIUtilities::wstringArray(JNIEnv* env, jobjectArray jarray)
{
    vector<wstring> elements;
    if (jarray) {
        int stringCount = env->GetArrayLength(jarray);
        for (int i = 0; i < stringCount; i++) {
			JNIString element(env, (jstring) env->GetObjectArrayElement(jarray, i));
			elements.push_back(element.c_str());
		}
    }
    return elements;
}

jobject JNIUtilities::newList(JNIEnv* env, size_t capacity)
{
	jclass listClass = JNIClass::getClass(env, "java/util/ArrayList");
	jobject list = env->NewObject(
		listClass,
		JNIClass::getMethodID(env, listClass, "<init>", "(I)V"), static_cast<jint>(capacity));
	if (env->ExceptionCheck()) throw JNIException(env);
	return list;
}

jobject JNIUtilities::newNativeObjectList(JNIEnv* env, size_t capacity)
{
	jclass listClass = JNIClass::getClass(env, "teaselib/core/jni/NativeObjectList");
	jobject list = env->NewObject(
		listClass,
		JNIClass::getMethodID(env, listClass, "<init>", "(I)V"), static_cast<jint>(capacity));
	if (env->ExceptionCheck()) throw JNIException(env);
	return list;
}

jobject JNIUtilities::asList(JNIEnv* env, const vector<string>& elements)
{
	jobject list = newList(env,	elements.size());
	jmethodID add = env->GetMethodID(JNIClass::getClass(env, "java/util/ArrayList"), "add", "(Ljava/lang/Object;)Z");
	for_each(elements.begin(), elements.end(), [&](const string& chars) {
		env->CallObjectMethod(list,	add, JNIStringUTF8(env, chars).operator jstring());
		if (env->ExceptionCheck()) throw JNIException(env);
	});
	return list;
}

jobject JNIUtilities::asList(JNIEnv* env, const vector<NativeObject*>& elements)
{
	jobject list = newNativeObjectList(env, elements.size());
	jmethodID add = env->GetMethodID(JNIClass::getClass(env, "teaselib/core/jni/NativeObjectList"), "add", "(Ljava/lang/Object;)Z");
	for_each(elements.begin(), elements.end(), [&](const NativeObject* nativeObject) {
		env->CallObjectMethod(list,	add, nativeObject->operator jobject());
		if (env->ExceptionCheck()) throw JNIException(env);
	});
	return list;
}

jobject JNIUtilities::asList(JNIEnv* env, const vector<jobject>& elements)
{
	jobject list = newList(env, elements.size());
	jmethodID add = env->GetMethodID(JNIClass::getClass(env, "java/util/ArrayList"), "add", "(Ljava/lang/Object;)Z");
	for_each(elements.begin(), elements.end(), [&](const jobject element) {
		env->CallObjectMethod(list, add, element);
		if (env->ExceptionCheck()) throw JNIException(env);
	});
	return list;
}


jobject JNIUtilities::newSet(JNIEnv* env) {
	jclass hashSetClass = JNIClass::getClass(env, "java/util/HashSet");
	jobject hashSet = env->NewObject(hashSetClass, JNIClass::getMethodID(env, hashSetClass, "<init>", "()V"));
	if (env->ExceptionCheck()) throw JNIException(env);
	return hashSet;
}

jobject JNIUtilities::asSet(JNIEnv* env, const set<NativeObject*>& elements)
{
	jclass setClass = JNIClass::getClass(env, "java/util/HashSet");
	jobject set = env->NewObject(
		setClass,
		JNIClass::getMethodID(env, setClass, "<init>", "(I)V"),
		elements.size());
	if (env->ExceptionCheck()) throw JNIException(env);

	jmethodID add = env->GetMethodID(setClass, "add", "(Ljava/lang/Object;)Z");
	for_each(elements.begin(), elements.end(), [&](const NativeObject* nativeObject) {
		env->CallObjectMethod(set, add, nativeObject->operator jobject());
		if (env->ExceptionCheck()) throw JNIException(env);
	});
	return set;
}

jobject JNIUtilities::enumValue(JNIEnv* env, const char* enumClass, const char* name)
{
	jclass cls = JNIClass::getClass(env, enumClass);
	jfieldID enumField = JNIClass::getStaticFieldID(env, cls, name, (string("L") + enumClass + ";").c_str());
	return env->GetStaticObjectField(cls, enumField);
}

vector<string> JNIUtilities::strings(JNIEnv* env, jobject jcollection)
{
	return list<string>(env, jcollection, [&env](jobject jelement)->string {
		 return JNIStringUTF8(env, (jstring) jelement).c_str();
	});
}

vector<wstring> JNIUtilities::wstrings(JNIEnv* env, jobject jcollection)
{
	return list<wstring>(env, jcollection, [&env](jobject jelement)->wstring {
		return JNIString(env, (jstring) jelement).c_str();
	});
}

vector<jobjectArray> JNIUtilities::objectArrays(JNIEnv* env, jobject jcollection)
{
	return list<jobjectArray>(env, jcollection, [&env](jobject jelement)->jobjectArray {
		return (jobjectArray) jelement;
	});
}
