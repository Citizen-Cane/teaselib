#include "stdafx.h"

#include "JNIArray.h"

template<> jsize JNIByteArray::getSize()
{
    return env->GetArrayLength(jthis);
}

template<> jbyte* JNIByteArray::getElements()
{
    return env->GetByteArrayElements(jthis, NULL);
}

template<> void JNIByteArray::releaseElements(int mode)
{
    env->ReleaseByteArrayElements(jthis, bytes, mode);
}
