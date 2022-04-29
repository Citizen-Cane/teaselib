#pragma once

#include<memory>
#include<vector>

#include <opencv2/core.hpp>

#include <NativeObject.h>

#include <Math/Image.h>
#include <Pose/Movenet.h>

class HumanPose {
public:
    HumanPose();
    virtual ~HumanPose();

    void setRotation(const aifx::image::Rotation rotation);
    bool acquire(JNIEnv* env, jobject jdevice);
    bool acquire(JNIEnv* env, jbyteArray jimage);
    const std::vector<aifx::pose::Pose>& estimate();

private:
    const aifx::pose::Movenet::Model model;

    typedef std::map<aifx::image::Orientation, aifx::pose::Movenet*> Models;
    Models models;

    aifx::image::Rotation rotation;
    cv::UMat frame;

    aifx::pose::Movenet* interpreter();
    std::vector<aifx::pose::Pose> poses;
};
