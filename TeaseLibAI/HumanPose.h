#pragma once

#include<memory>
#include<vector>

#include <NativeObject.h>

#include <Pose/AIfxPoseEstimation.h>

class HumanPose {
public:
    HumanPose();
    virtual ~HumanPose();

    bool acquire(JNIEnv* env, jobject jdevice, jobject jrotation);
    bool acquire(JNIEnv* env, jbyteArray jimage);
    void estimate();
    const std::vector<aifx::Pose>& results();

private:
    const aifx::PoseEstimation::Model model;
    const aifx::PoseEstimation::Resolution resolution;

    typedef std::map<aifx::PoseEstimation::Rotation, aifx::PoseEstimation*> InterpreterMap;
    InterpreterMap interpreterMap;

    aifx::PoseEstimation::Rotation rotation;
    cv::UMat frame;

    aifx::PoseEstimation* interpreter();
    std::vector<aifx::Pose> poses;
};
