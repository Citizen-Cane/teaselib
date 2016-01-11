#include "stdafx.h"

#include <assert.h>

#include "COMUser.h"

//unsigned long COMUser::refCount = 0;
COMUser::RefCount COMUser::refCount;

// TODO Not thread safe, need to init per thread
COMUser::COMUser()
{
	const DWORD thread = GetCurrentThreadId();
	if (refCount[thread]++ == 0)
	{
		if (FAILED(::CoInitialize(NULL)))
		{
			assert(false);
		}
	}
}

COMUser::COMUser(std::tr1::function<void()> code)
: COMUser()
{
	code();
}

COMUser::~COMUser()
{
	const DWORD thread = GetCurrentThreadId();
	if (--refCount[thread] == 0)
	{
		::CoUninitialize();
	}
}
