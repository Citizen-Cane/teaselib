#pragma once

#include <NativeObject.h>

class SceneCapture : public NativeObject {
public:
	SceneCapture(JNIEnv* env, const aifx::video::VideoCapture::CameraInfo& cameraInfo);
private:
	std::unique_ptr<aifx::video::VideoCapture> device;
};
