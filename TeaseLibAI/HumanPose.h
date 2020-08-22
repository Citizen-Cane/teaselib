#pragma once

#include<memory>
#include<vector>

#include <NativeObject.h>

#include <AIfxVideoCapture.h>
#include <AIfxPoseEstimation.h>

class HumanPose : public NativeObject {
public:
    HumanPose(JNIEnv* env, jobject jdevice, jobject jrotation);
    virtual ~HumanPose();

    bool acquire();
    void estimate();
    const std::vector<aifx::Pose>& results();

private:
    jobject jdevice;
    std::unique_ptr<aifx::VideoCapture> capture;
    aifx::PoseEstimation poseEstimation;

    cv::UMat frame;
    std::vector<aifx::Pose> poses;
};