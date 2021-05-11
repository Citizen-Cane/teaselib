#pragma once

#include <jni.h>

#include "JNIObject.h"

template <typename E,typename A> class JNIArray : public JNIObject<A>
{
public:
	JNIArray(JNIEnv* env, A jarray) 
		: JNIObject<A>(env, jarray)
		, size(getSize())
		, bytes(size ? getElements() : nullptr)
	{}
		
	~JNIArray()
	{
		releaseElements(JNI_ABORT);
	}

	const jsize size;
	E* const bytes;

	operator E* () { return bytes; };
	operator E* () const { return bytes; };

	void commit()
	{
		Objects::requireNonNull(L"bytes", bytes);
		releaseElements(JNI_COMMIT);
	}

protected:
	TEASELIB_FRAMEWORK_EXPORT jsize getSize();
	TEASELIB_FRAMEWORK_EXPORT E* getElements();
	TEASELIB_FRAMEWORK_EXPORT void releaseElements(int mode);
};

template JNIArray<jbyte, jbyteArray>;
typedef JNIArray<jbyte, jbyteArray> JNIByteArray;
