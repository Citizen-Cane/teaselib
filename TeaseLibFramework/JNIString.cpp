#include "stdafx.h"
#include "JNIString.h"


JNIString::JNIString(JNIEnv *env, jstring string)
: JNIObject(env, string)
{
	jboolean isCopy;
	this->string = reinterpret_cast<const wchar_t*>(env->GetStringChars(string, &isCopy));
}

JNIString::JNIString(JNIEnv *env, const wchar_t * const string)
: JNIObject(env, env->NewString(reinterpret_cast<const jchar*>(string), _tcslen(string)))
{
	jboolean isCopy;
	this->string = reinterpret_cast<const wchar_t*>(env->GetStringChars(static_cast<jstring>(jthis), &isCopy));
}

JNIString::~JNIString()
{
	env->ReleaseStringChars(static_cast<jstring>(jthis), reinterpret_cast<const jchar*>(string));
}

JNIString::operator const wchar_t*() const
{
	return string;
}
