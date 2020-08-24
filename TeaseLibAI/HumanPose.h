#pragma once

#include<memory>
#include<vector>

#include <NativeObject.h>

#include <AIfxPoseEstimation.h>

class HumanPose : public NativeObject {
public:
    HumanPose(JNIEnv* env);
    virtual ~HumanPose();

    bool acquire(jobject jdevice, jobject jrotation);
    bool acquire(jbyteArray jimage);
    void HumanPose::estimate();
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
