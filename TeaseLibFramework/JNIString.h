#pragma once

#include "JNIObject.h"

template<typename T> class JNIStringT : public JNIObject<jstring>
{
public:
	JNIStringT(JNIEnv *env, jstring string);
	JNIStringT(JNIEnv *env, const T * const string);
	virtual ~JNIStringT();

	operator const T*() const {
		return string;
	};
	size_t length() const {
		return env->GetStringLength(jthis);
	};
private:
	const T *string;
};

typedef JNIStringT<wchar_t> JNIString;
typedef JNIStringT<char> JNIStringUTF8;
