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

        MultiPose = 256
    };

    void set(Interest interests);
    void set(const aifx::image::Rotation rotation);
    bool acquire(aifx::video::VideoCapture* capture);
    bool acquire(const void* image, int size);
    const std::vector<aifx::pose::Pose>& estimate(const std::chrono::milliseconds timestamp = 0ms);

    static jobject estimation(JNIEnv* env, const aifx::pose::Pose& pose);
private:
    typedef std::map<int, aifx::pose::Movenet*> Models;
    Models models;
    int interests;

    aifx::image::Rotation rotation;
    cv::UMat frame;

    aifx::pose::Movenet* interpreter();
    std::vector<aifx::pose::Pose> poses;
};

