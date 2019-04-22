#include "stdafx.h"

#include <assert.h>

#include "COMException.h"
#include "COMUser.h"

COMUser::RefCount COMUser::refCount;

COMUser::COMUser()
	: threadId(GetCurrentThreadId()) {
	auto initiailized = refCount.find(threadId);
	if (initiailized == refCount.end()) {
		HRESULT hr = ::CoInitialize(NULL);
		if (FAILED(hr)) {
			throw new COMException(hr);
		}
		refCount.insert({ threadId, 1 });
	} else {
		refCount[threadId]++;
	}
}

COMUser::COMUser(std::function<void()> code) 
: COMUser() {
	code();
}

COMUser::~COMUser() {
	if (--refCount[threadId] == 0) {
		::CoUninitialize();
		refCount.erase(threadId);
	}
}

void COMUser::checkThread() const {
	if (threadId != GetCurrentThreadId()) {
		assert(false);
		throw new COMException(E_UNEXPECTED, L"COM-code called from wrong thread");
	}
}
