#pragma once

#include <functional>
#include <map>

class COMUser
{
public:
	COMUser();
	COMUser(std::function<void()> code);
	virtual ~COMUser();
protected:
	typedef std::map<DWORD, unsigned long> RefCount;
	static RefCount refCount;
};

