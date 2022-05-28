#pragma once

#include<chrono>
#include<memory>
#include<vector>

#include <opencv2/core.hpp>

#include <NativeObject.h>

#include <Math/Image.h>
#include <Pose/Movenet.h>
#include <Video/VideoCapture.h>

using namespace std::chrono_literals;

class HumanPose {
public:
    HumanPose();
    virtual ~HumanPose();

    enum Interest {
        Status = 1,
        Proximity = 2,
        HeadGestures = 4,

        UpperTorso = 8,
        LowerTorso = 16,
        LegsAndFeet = 32,
    };

    void set(Interest interests);
    void set(const aifx::image::Rotation rotation);
    bool acquire(aifx::video::VideoCapture* capture);
    bool acquire(const void* image, int size);
    const std::vector<aifx::pose::Pose>& estimate(const std::chrono::milliseconds timestamp = 0ms);

private:
    const aifx::pose::Movenet::Model model;

    typedef std::map<aifx::image::Orientation, aifx::pose::Movenet*> Models;
    Models models;
    int interests;

    aifx::image::Rotation rotation;
    cv::UMat frame;

    aifx::pose::Movenet* interpreter();
    std::vector<aifx::pose::Pose> poses;
};

jobject jpose(JNIEnv* env, const aifx::pose::Pose& pose);
