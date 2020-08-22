#include "pch.h"

#include<vector>

#include <opencv2/core.hpp>

#include <JNIObject.h>
#include <JNIUtilities.h>

#include <teaselib_core_ai_perception_SceneCapture.h>
#include <teaselib_core_ai_perception_HumanPose.h>

#include <Pose.h>

#include "HumanPose.h"

using namespace aifx;
using namespace cv;
using namespace std;

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
 * Method:    acquire
 * Signature: ()Z
 */
	JNIEXPORT jboolean JNICALL Java_teaselib_core_ai_perception_HumanPose_acquire
	(JNIEnv* env, jobject jthis)
	{
		try {
			HumanPose* humanPose = static_cast<HumanPose*>(NativeObject::get(env, jthis));
			return humanPose->acquire();
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    estimate
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_HumanPose_estimate
	(JNIEnv* env, jobject jthis)
	{
		try {
			HumanPose* humanPose = static_cast<HumanPose*>(NativeObject::get(env, jthis));
			std::set<NativeObject*> aspects;

			humanPose->estimate();
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
	}
}

/*
 * Class:     teaselib_core_ai_perception_HumanPose
 * Method:    results
 * Signature: ()Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_teaselib_core_ai_perception_HumanPose_results
(JNIEnv* env, jobject jthis)
{
	try {
		HumanPose* humanPose = static_cast<HumanPose*>(NativeObject::get(env, jthis));
		vector<Pose> poses = humanPose->results();

		// TODO build list of HumanPose.Result
		vector<jobject> results;
		for_each(poses.begin(), poses.end(), [&env, &results] (const aifx::Pose& pose) {
			const Point3f gaze = pose.gaze();
			jclass resultClass = JNIClass::getClass(env, "teaselib/core/ai/perception/HumanPose$EstimationResult");
			if (env->ExceptionCheck()) throw JNIException(env);
			jobject jpose = env->NewObject(
				resultClass,
				JNIClass::getMethodID(env, resultClass, "<init>", "(FFFF)V"),
				pose.distance,
				gaze.x,
				gaze.y,
				gaze.z
			);
			if (env->ExceptionCheck()) throw JNIException(env);

			results.push_back(jpose);
		});

		return JNIUtilities::asList(env, results);
	}
	catch (NativeException& e) {
		JNIException::throwNew(env, e);
		return nullptr;
	}
	catch (JNIException& e) {
		e.rethrow();
		return nullptr;
	}
}


HumanPose::HumanPose(JNIEnv* env, jobject jdevice, jobject jrotation)
	: NativeObject(env)
	, jdevice(env->NewGlobalRef(jdevice))
	, capture(reinterpret_cast<aifx::VideoCapture*>(NativeObject::get(env, jdevice)))
	, poseEstimation(
		PoseEstimation::Model::MobileNetThin,
		PoseEstimation::Resolution::Size128x96,
		TfLiteInterpreter::ComputationMode::GPU_CPU,
		// TODO update java code to retrieve device orientation from device and evaluate jrotation
		jrotation != nullptr ? PoseEstimation::Rotation::Clockwise : PoseEstimation::Rotation::None)
{}

HumanPose::~HumanPose()
{
	env->DeleteGlobalRef(jdevice);
}

bool HumanPose::acquire()
{
	if (capture->started()) {
		*capture >> frame;
		return !frame.empty();
	} else {
		return false;
	}
}

void HumanPose::estimate()
{
	poseEstimation.setInputTensor(frame);
	poseEstimation.invoke();
}

const vector<Pose>& HumanPose::results()
{
	poses = poseEstimation.results();
	return poses;
}
