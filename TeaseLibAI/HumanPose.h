#pragma once

#include<memory>
#include<vector>

#include <opencv2/core.hpp>

#include <NativeObject.h>

#include <Pose/PoseEstimation.h>

class HumanPose {
public:
    HumanPose();
    virtual ~HumanPose();

    bool acquire(JNIEnv* env, jobject jdevice, jobject jrotation);
    bool acquire(JNIEnv* env, jbyteArray jimage);
    void estimate();
    const std::vector<aifx::pose::Pose>& results();

private:
    const aifx::pose::PoseEstimation::Model model;
    const aifx::pose::PoseEstimation::Resolution resolution;

    typedef std::map<aifx::pose::PoseEstimation::Rotation, aifx::pose::PoseEstimation*> InterpreterMap;
    InterpreterMap interpreterMap;

    aifx::pose::PoseEstimation::Rotation rotation;
    cv::UMat frame;

    aifx::pose::PoseEstimation* interpreter();
    std::vector<aifx::pose::Pose> poses;
};
