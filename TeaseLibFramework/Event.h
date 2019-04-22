#pragma once

#include <jni.h>

class Event {
public:
	Event(JNIEnv *env, jobject jevent, const char* name);
	virtual ~Event();
protected:
	void fire(jobject eventArgs);

	JNIEnv *env;
	jobject jevent;
	const char* name;
};

