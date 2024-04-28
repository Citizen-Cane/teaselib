#pragma once

#include <NativeObject.h>

#include <video/VideoCapture.h>

class SceneCapture {
public:
	static SceneCapture* nativeInstance(JNIEnv* env, jobject jthis);

	SceneCapture(aifx::video::VideoCapture* device);
	~SceneCapture();
	aifx::video::VideoCapture* device;
};
