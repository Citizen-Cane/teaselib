#pragma once

#include <COMUser.h>
#include <NativeObject.h>

#include "Voice.h"


class SpVoice : public Voice, protected COMUser
{
public:
	SpVoice(JNIEnv* env, jobject ttsImpl, ISpObjectToken* pVoiceToken);
	virtual ~SpVoice();

	operator ISpObjectToken*() const;
private:
	ISpObjectToken* pVoiceToken;
	HRESULT SpGetAttribute(_In_ ISpObjectToken * pObjToken, _In_ LPCWSTR pAttributeName, _Outptr_ PWSTR *ppszDescription);
};
