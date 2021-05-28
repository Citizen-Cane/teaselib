#include "stdafx.h"

#include <string>

#include "JNIString.h"


template<> JNIStringT<wchar_t>::JNIStringT(JNIEnv *env, jstring string) 
	: JNIObject(env, string) {
	jboolean isCopy;
	this->string = reinterpret_cast<const wchar_t*>(env->GetStringChars(string, &isCopy));
}

template<> JNIStringT<wchar_t>::JNIStringT(JNIEnv *env, const wchar_t * const string)
	: JNIObject(env, env->NewString(reinterpret_cast<const jchar*>(string), static_cast<jsize>(wcslen(string)))) {
	jboolean isCopy;
	this->string = reinterpret_cast<const wchar_t*>(env->GetStringChars(jthis, &isCopy));
}

template<> JNIStringT<wchar_t>::JNIStringT(JNIEnv* env, const std::basic_string<wchar_t>& string)
	: JNIObject(env, env->NewString(reinterpret_cast<const jchar*>(string.c_str()), static_cast<jsize>(string.size()))) {
	jboolean isCopy;
	this->string = reinterpret_cast<const wchar_t*>(env->GetStringChars(jthis, &isCopy));
}

template<> JNIStringT<wchar_t>::~JNIStringT() {
	env->ReleaseStringChars(jthis, reinterpret_cast<const jchar*>(string));
}

template<> JNIStringT<char>::JNIStringT(JNIEnv *env, jstring string)
	: JNIObject(env, string) {
	jboolean isCopy;
	this->string = env->GetStringUTFChars(string, &isCopy);
}

template<> JNIStringT<char>::JNIStringT(JNIEnv *env, const char * const string)
	: JNIObject(env, env->NewStringUTF(reinterpret_cast<const char*>(string))) {
	jboolean isCopy;
	this->string = env->GetStringUTFChars(jthis, &isCopy);
}

template<> JNIStringT<char>::JNIStringT(JNIEnv* env, const std::basic_string<char>& string)
	: JNIObject(env, env->NewStringUTF(reinterpret_cast<const char*>(string.c_str()))) {
	jboolean isCopy;
	this->string = env->GetStringUTFChars(jthis, &isCopy);
}

template<> JNIStringT<char>::~JNIStringT() {
	env->ReleaseStringUTFChars(jthis, string);
}

template class JNIStringT<wchar_t>;
template class JNIStringT<char>;