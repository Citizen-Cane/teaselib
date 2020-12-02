#include "pch.h"

#include<algorithm>
#include<exception>
#include<stdexcept>
#include<vector>

#include <opencv2/core.hpp>
#include <opencv2/imgcodecs.hpp>

#include <JNIArray.h>
#include <JNIObject.h>
#include <JNIUtilities.h>


#include <AIfxPoseEstimation.h>
#include <AIfxVideoCapture.h>
#include <TfLiteDelegateV2.h>

#include <Pose.h>

#include <teaselib_core_ai_perception_HumanPose.h>
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
		catch (std::invalid_argument& e) {
			JNIException::rethrow(env, e);
			return 0;
		}
		catch (std::exception& e) {
			JNIException::rethrow(env, e);
			return 0;
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
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
	(JNIEnv* /*env*/, jobject /*jthis*/, jint /*interestMask*/) {
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
		catch (std::invalid_argument& e) {
			JNIException::rethrow(env, e);
			return false;
		}
		catch (std::exception& e) {
			JNIException::rethrow(env, e);
			return false;
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
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
			Objects::requireNonNull(L"image", jimage);

			HumanPose* humanPose = static_cast<HumanPose*>(NativeObject::get(env, jthis));
			return humanPose->acquire(jimage);
		}
		catch (std::invalid_argument& e) {
			JNIException::rethrow(env, e);
			return false;
		}
		catch (std::exception& e) {
			JNIException::rethrow(env, e);
			return false;
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
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
		catch (std::invalid_argument& e) {
			JNIException::rethrow(env, e);
		}
		catch (std::exception& e) {
			JNIException::rethrow(env, e);
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
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
			const Point2f head = pose.head();
			const Point3f gaze = pose.gaze();
			jclass resultClass = JNIClass::getClass(env, "teaselib/core/ai/perception/HumanPose$Estimation");
			if (env->ExceptionCheck()) throw JNIException(env);
			jobject jpose;
			if (isnan(pose.distance)) {
				jpose = env->NewObject(
					resultClass,
					JNIClass::getMethodID(env, resultClass, "<init>", "()V")
				);
			} else
			if (isnan(head.x) || isnan(head.y)) {
				jpose = env->NewObject(
					resultClass,
					JNIClass::getMethodID(env, resultClass, "<init>", "(F)V"),
					pose.distance
				);
			} else
			if (isnan(gaze.x) || isnan(gaze.y) || isnan(gaze.z)) {
				jpose = env->NewObject(
					resultClass,
					JNIClass::getMethodID(env, resultClass, "<init>", "(FFF)V"),
					pose.distance,
					head.x,
					head.y
				);
			} else {
				jpose = env->NewObject(
					resultClass,
					JNIClass::getMethodID(env, resultClass, "<init>", "(FFFFFF)V"),
					pose.distance,
					head.x,
					head.y,
					gaze.x,
					gaze.y,
					gaze.z
				);
			}
			if (env->ExceptionCheck()) throw JNIException(env);

			results.push_back(jpose);
		});

		return JNIUtilities::asList(env, results);
	}
	catch (std::invalid_argument& e) {
		JNIException::rethrow(env, e);
		return nullptr;
	}
	catch (std::exception& e) {
		JNIException::rethrow(env, e);
		return nullptr;
	}
	catch (NativeException& e) {
		JNIException::rethrow(env, e);
		return nullptr;
	}
	catch (JNIException& e) {
		e.rethrow();
		return nullptr;
	}
}


HumanPose::HumanPose(JNIEnv* env)
	: NativeObject(env)
	, model(PoseEstimation::Model::MobileNetThin_Gpu_Resize)
	, resolution(PoseEstimation::Resolution::Size320x240)
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
	JNIByteArray image(env, jimage);
	if (image.size == 0) {
		throw invalid_argument("no bytes");
	} else {
		Mat data(1, image.size, CV_8UC1, (void*)image.bytes);
		imdecode(data, IMREAD_COLOR).copyTo(frame);
		if (frame.empty()) {
			throw invalid_argument("not an image");
		}
		return true;
	}
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
			new TfLiteDelegateV2,
			rotation);
	}
	return interpreter;
}
