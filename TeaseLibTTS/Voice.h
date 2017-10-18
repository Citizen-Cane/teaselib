#pragma once

#include <COMUser.h>
#include <NativeObject.h>

class Voice : public NativeObject, protected COMUser
{
public:
	Voice(JNIEnv* env, ISpObjectToken* pVoiceToken, jobject ttsImpl);
	virtual ~Voice();
	operator ISpObjectToken*() const;
	std::wstring guid;
private:
	ISpObjectToken* pVoiceToken;
	HRESULT SpGetAttribute(_In_ ISpObjectToken * pObjToken, _In_ LPCWSTR pAttributeName, _Outptr_ PWSTR *ppszDescription);
};
