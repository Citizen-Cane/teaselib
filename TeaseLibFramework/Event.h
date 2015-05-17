#pragma once

#include <jni.h>

class Event
{
public:
	Event(JNIEnv *env, jobject sender, jobject jevent, const char* name);
	virtual ~Event();

protected:
	void fire(jobject eventArgs);

	JNIEnv *env;
	jobject sender;
	jobject jevent;
	const char* name;
};

