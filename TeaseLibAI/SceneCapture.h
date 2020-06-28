#pragma once

#include <NativeObject.h>

class SceneCapture : public NativeObject {
public:
	SceneCapture(JNIEnv* env, int id, const char* name);
private:
	std::unique_ptr<aifx::VideoCapture> device;
};