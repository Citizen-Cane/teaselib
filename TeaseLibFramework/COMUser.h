#pragma once

#include <functional>
#include <map>

class COMUser
{
public:
	COMUser();
	COMUser(std::function<void()> code);
	virtual ~COMUser();
	void checkThread() const;
protected:
	const DWORD threadId;
	typedef std::map<DWORD, unsigned long> RefCount;
	static RefCount refCount;
};

