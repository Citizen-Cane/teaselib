#include "pch.h"

#include<vector>

#include <JNIObject.h>
#include <JNIUtilities.h>

#include <teaselib_core_ai_perception_SceneCapture.h>
#include <teaselib_core_ai_perception_HumanPose.h>

#include "HumanPose.h"

using namespace aifx;
using namespace cv;

extern "C"
{

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    init
	 * Signature: (Lteaselib/core/ai/perception/SceneCapture;)J
	 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_perception_HumanPose_init
	(JNIEnv* env, jclass, jobject jdevice, jobject jrotation) {
		try {
			Objects::requireNonNull(L"device", jdevice);
			HumanPose* humanPose = new HumanPose(env, jdevice, jrotation);
			return reinterpret_cast<jlong>(humanPose);
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
		return 0;
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    setInterests
	 * Signature: (I)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_HumanPose_setInterests
	(JNIEnv* env, jobject jthis, jint interestMask) {
		// TODO choose inference model according to desired aspects
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    estimatePose
	 * Signature: ()I
	 */
	JNIEXPORT jint JNICALL Java_teaselib_core_ai_perception_HumanPose_estimatePose
	(JNIEnv* env, jobject jthis) {
		try {
			HumanPose* humanPose = static_cast<HumanPose*>(NativeObject::get(env, jthis));
			std::set<NativeObject*> aspects;
			return humanPose->estimate();
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
		return 0;
	}
}


HumanPose::HumanPose(JNIEnv* env, jobject jdevice, jobject jrotation)
	: NativeObject(env)
	, jdevice(env->NewGlobalRef(jdevice))
	, capture(reinterpret_cast<aifx::VideoCapture*>(NativeObject::get(env, jdevice)))
	, poseEstimation(
		PoseEstimation::Model::MobileNetThin,
		PoseEstimation::Resolution::Size128x96,
		TfLiteInterpreter::ComputationMode::CPU,
		// TODO update java code to retrieve device orientation and evaluate jrotation
		jrotation != nullptr ? PoseEstimation::Rotation::Clockwise : PoseEstimation::Rotation::None)
{}

HumanPose::~HumanPose()
{
	env->DeleteGlobalRef(jdevice);
}

int HumanPose::estimate()
{
	if (capture->started()) {
		*capture >> frame;
		if (frame.empty()) {
			return teaselib_core_ai_perception_SceneCapture_NoImage;
		}
		else {
			return poseEstimation(frame);
		}
	} else {
		return teaselib_core_ai_perception_SceneCapture_NoImage;
	}
}
