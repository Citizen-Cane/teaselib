#pragma once

#include "JNIObject.h"

template<typename T> class TEASELIB_FRAMEWORK_EXPORT JNIStringT : public JNIObject<jstring>
{
public:
	JNIStringT(JNIEnv *env, jstring string);
	JNIStringT(JNIEnv* env, const T* const string);
	JNIStringT(JNIEnv* env, const std::basic_string<T>& string);
	virtual ~JNIStringT();

	operator const T*() const {
		return string;
	};

	const T* c_str() const {
		return string;
	};

	size_t length() const {
		return env->GetStringLength(jthis);
	};

	bool operator==(const T* other) const;

private:
	const T *string;
};

typedef JNIStringT<wchar_t> JNIString;
typedef JNIStringT<char> JNIStringUTF8;
