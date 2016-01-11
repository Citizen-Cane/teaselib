#pragma once

#include <functional>
#include <map>

class COMUser
{
//	static unsigned long refCount;
public:
	COMUser();
	COMUser(std::tr1::function<void()> code);
	virtual ~COMUser();
protected:
	typedef std::map<DWORD, unsigned long> RefCount;
	static RefCount refCount;
};

