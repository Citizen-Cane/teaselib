#pragma once

#include "JNIObject.h"

template <typename ARRAYTYPE,typename OBJECTTYPE> class JNIArray : public JNIObject<OBJECTTYPE>
{
public:
	JNIArray(JNIEnv* env, OBJECTTYPE jarray) : JNIObject(env, jarray), size(getSize()), bytes(size ? getElements() : nullptr)
	{}
		
	~JNIArray()
	{
		releaseElements(JNI_ABORT);
	}

	const jsize size;
	ARRAYTYPE* const bytes;

	operator ARRAYTYPE* () { return bytes; };
	operator ARRAYTYPE* () const { return bytes; };

	void commit()
	{
		Objects::requireNonNull(L"bytes", bytes);
		releaseElements(JNI_COMMIT);
	}

protected:
	jsize getSize();
	ARRAYTYPE* getElements();
	void releaseElements(int mode);
};

template JNIArray<jbyte, jbyteArray>;
typedef JNIArray<jbyte, jbyteArray> JNIByteArray;
