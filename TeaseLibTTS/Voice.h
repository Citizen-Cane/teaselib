#pragma once

#include <jni.h>

#include <JNIObject.h>
#include <NativeObject.h>

class Voice : public NativeObject {
public:
	Voice(JNIEnv* env);
	virtual ~Voice();

protected:
	jobject getGenderField(const char* gender);
	jobject getGenderField(const wchar_t* gender);
	jobject newVoiceInfo(const JNIObject<jstring>& vendor, const JNIObject<jstring>& language, const JNIObject<jstring>& name);
	jobject newNativeVoice(jobject ttsImpl, const JNIObject<jstring>& guid, jobject jgender, const JNIObject<jstring>& locale, jobject jvoiceInfo);
};
