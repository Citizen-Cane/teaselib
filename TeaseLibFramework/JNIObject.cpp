#include "stdafx.h"

#include "NativeException.h"
#include "JNIObject.h"

void Objects::requireNonNull(const wchar_t* name, jobject jobj)
{
	if (jobj == nullptr) throw NativeException(E_POINTER, name, "Ljava/lang/NullPointerException;");
}
