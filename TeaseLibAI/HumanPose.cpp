#include "pch.h"

#include<algorithm>
#include<vector>

#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>

#include <JNIObject.h>
#include <JNIUtilities.h>

#include <teaselib_core_ai_perception_SceneCapture.h>
#include <teaselib_core_ai_perception_HumanPose.h>

#include <AIfxPoseEstimation.h>
#include <AIfxVideoCapture.h>

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
	 * Signature: ()J
	 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_perception_HumanPose_init
	(JNIEnv* env, jclass) {
		try {
			HumanPose* humanPose = new HumanPose(env);
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
	 * Signature: (Lteaselib/core/ai/perception/SceneCapture;Lteaselib/core/ai/perception/SceneCapture/Rotation;)Z
	 */
	JNIEXPORT jboolean JNICALL Java_teaselib_core_ai_perception_HumanPose_acquire
	(JNIEnv* env, jobject jthis, jobject jdevice, jobject jrotation)
	{
		try {
			Objects::requireNonNull(L"device", jdevice);
			//Objects::requireNonNull(L"device", jrotation);

			HumanPose* humanPose = static_cast<HumanPose*>(NativeObject::get(env, jthis));
			return humanPose->acquire(jdevice, jrotation);
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
			return false;
		}
		catch (JNIException& e) {
			e.rethrow();
			return false;
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    acquire
	 * Signature: ([B)Z
	 */
	JNIEXPORT jboolean JNICALL Java_teaselib_core_ai_perception_HumanPose_acquireImage
	(JNIEnv* env, jobject jthis, jbyteArray jimage) {
		try {
			Objects::requireNonNull(L"device", jimage);

			HumanPose* humanPose = static_cast<HumanPose*>(NativeObject::get(env, jthis));
			return humanPose->acquire(jimage);
		}
		catch (NativeException& e) {
			JNIException::throwNew(env, e);
			return false;
		}
		catch (JNIException& e) {
			e.rethrow();
			return false;
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


HumanPose::HumanPose(JNIEnv* env)
	: NativeObject(env)
	, model(PoseEstimation::Model::MobileNetThin)
	, resolution(PoseEstimation::Resolution::Size128x96)
	, rotation(PoseEstimation::Rotation::None)
	{}

HumanPose::~HumanPose()
{
	for_each(interpreterMap.begin(), interpreterMap.end(), [](const InterpreterMap::value_type& interpreter) {
		delete interpreter.second;
	});
}

bool HumanPose::acquire(jobject jdevice, jobject jrotation)
{
	rotation = jrotation != nullptr ? PoseEstimation::Rotation::Clockwise : PoseEstimation::Rotation::None;

	aifx::VideoCapture* capture = reinterpret_cast<aifx::VideoCapture*>(NativeObject::get(env, jdevice));
	if (capture->started()) {
		*capture >> frame;
		return !frame.empty();
	} else {
		return false;
	}
}

bool HumanPose::acquire(jbyteArray jimage)
{
	jbyte* image = (jbyte*)env->GetByteArrayElements(jimage, NULL);
	jsize size = env->GetArrayLength(jimage);
	Mat data(1, size, CV_8UC1, (void*)image);
	imdecode(data, IMREAD_COLOR).copyTo(frame);
	env->ReleaseByteArrayElements(jimage, image, 0);
	return true;
}

void HumanPose::estimate()
{
	PoseEstimation* poseEstimation = interpreter();
	poseEstimation->setInputTensor(frame);
	poseEstimation->invoke();
}

const vector<Pose>& HumanPose::results()
{
	PoseEstimation* poseEstimation = interpreter();
	poses = poseEstimation->results();
	return poses;
}

aifx::PoseEstimation* HumanPose::interpreter()
{
	PoseEstimation* interpreter = interpreterMap[rotation];
	if (interpreter == nullptr) {
		interpreterMap[rotation] = interpreter =new PoseEstimation(
			model,
			resolution,
			TfLiteInterpreter::ComputationMode::CPU,
			rotation);
	}
	return interpreter;
}
