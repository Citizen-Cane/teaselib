#include "stdafx.h"

#include <JNIClass.h>

#include "Voice.h"

Voice::Voice(JNIEnv* env)
    : NativeObject(env)
{
}

Voice::~Voice()
{}

jobject Voice::getGenderField(const char* gender)
{
    const char* genderFieldName;
    if (_stricmp(gender, "Female") == 0) {
        genderFieldName = "Female";
    } else if (_stricmp(gender, "Male") == 0) {
        genderFieldName = "Male";
    } else {
        genderFieldName = "Robot";
    }

    jclass voiceClass = JNIClass::getClass(env, "teaselib/core/texttospeech/Voice");
    jobject genderValue = env->GetStaticObjectField(
        voiceClass,
        JNIClass::getStaticFieldID(env, voiceClass, genderFieldName, "Lteaselib/Sexuality$Gender;"));
    if (env->ExceptionCheck()) throw JNIException(env);

    return genderValue;
}

jobject Voice::getGenderField(const wchar_t* gender)
{
    const char* genderFieldName;
    if (_wcsicmp(gender, L"Female") == 0) {
        genderFieldName = "Female";
    } else if (_wcsicmp(gender, L"Male") == 0) {
        genderFieldName = "Male";
    } else {
        genderFieldName = "Robot";
    }

    jclass voiceClass = JNIClass::getClass(env, "teaselib/core/texttospeech/Voice");
    jobject genderValue = env->GetStaticObjectField(
        voiceClass,
        JNIClass::getStaticFieldID(env, voiceClass, genderFieldName, "Lteaselib/Sexuality$Gender;"));
    if (env->ExceptionCheck()) throw JNIException(env);

    return genderValue;
}

jobject Voice::newVoiceInfo(const JNIObject<jstring>& vendor, const JNIObject<jstring>& language, const JNIObject<jstring>& name)
{
	jclass clazz = JNIClass::getClass(env, "teaselib/core/texttospeech/VoiceInfo");
    jobject jvoiceInfo = env->NewObject(
        clazz,
        JNIClass::getMethodID(env, clazz, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V"),
        env->NewLocalRef(vendor),
        env->NewLocalRef(language),
        env->NewLocalRef(name));
    if (env->ExceptionCheck()) throw JNIException(env);
    return jvoiceInfo;
}

jobject Voice::newNativeVoice(jobject ttsImpl, const JNIObject<jstring>& guid, jobject jgender, const JNIObject<jstring>& locale, jobject jvoiceInfo)
{
    jclass clazz = env->FindClass("teaselib/core/texttospeech/NativeVoice");
    if (env->ExceptionCheck()) throw JNIException(env);

    const char* signature = "(JLteaselib/core/texttospeech/TextToSpeechImplementation;Ljava/lang/String;Ljava/lang/String;Lteaselib/Sexuality$Gender;Lteaselib/core/texttospeech/VoiceInfo;)V";
    jobject jvoice = env->NewObject(
        clazz,
        JNIClass::getMethodID(env, clazz, "<init>", signature),
        reinterpret_cast<jlong>(this),
        ttsImpl,
        env->NewLocalRef(guid),
        env->NewLocalRef(locale),
        jgender,
        jvoiceInfo);
    if (env->ExceptionCheck()) throw JNIException(env);
    return jvoice;
}
