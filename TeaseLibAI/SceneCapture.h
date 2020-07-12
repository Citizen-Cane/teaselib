#pragma once

#include <NativeObject.h>

class SceneCapture : public NativeObject {
public:
	SceneCapture(JNIEnv* env, const aifx::VideoCapture::CameraInfo& cameraInfo);
private:
	std::unique_ptr<aifx::VideoCapture> device;
};