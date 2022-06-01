#pragma once

#include <Pose/Person.h>

class Human
{
public:
	static void update(JNIEnv* env, jobject jperson, const aifx::pose::Pose& pose);
	aifx::pose::Person person;
};
