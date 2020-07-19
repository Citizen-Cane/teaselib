#pragma once

#include<memory>

#include <NativeObject.h>

#include <AIfxVideoCapture.h>
#include <AIfxPoseEstimation.h>

class HumanPose : public NativeObject {
public:
    HumanPose(JNIEnv* env, jobject jdevice, jobject jrotation);
    virtual ~HumanPose();

    int estimate();

private:
    jobject jdevice;
    std::unique_ptr<aifx::VideoCapture> capture;
    aifx::PoseEstimation poseEstimation;

    cv::UMat frame;
};