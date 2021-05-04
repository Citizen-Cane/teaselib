#pragma once

#include <functional>
#include <map>

class COMUser
{
public:
	TEASELIB_FRAMEWORK_EXPORT COMUser();
	TEASELIB_FRAMEWORK_EXPORT COMUser(std::function<void()> code);
	TEASELIB_FRAMEWORK_EXPORT virtual ~COMUser();
	TEASELIB_FRAMEWORK_EXPORT void checkThread() const;
protected:
	const DWORD threadId;
private:
	typedef std::map<DWORD, unsigned long> RefCount;
	static RefCount refCount;
};

