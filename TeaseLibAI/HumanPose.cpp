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

#include <TensorFlow/TfLiteDelegateV2/TfLiteDelegateV2.h>
#include <Pose/Movenet.h>
#include <Pose/Pose.h>
#include <Video/VideoCapture.h>

#include <teaselib_core_ai_perception_HumanPose.h>
#include "HumanPose.h"

using namespace aifx;
using namespace aifx::pose;
using namespace aifx::tensorflow;
using namespace aifx::video;

using namespace cv;
using namespace std;

extern "C"
{

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    init
	 * Signature: ()J
	 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_perception_HumanPose_newNativeInstance
	(JNIEnv* env, jclass) {
		try {
			HumanPose* humanPose = new HumanPose();
			return reinterpret_cast<jlong>(humanPose);
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
			return 0;
		} catch (exception& e) {
			JNIException::rethrow(env, e);
			return 0;
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
		return 0;
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    loadModel
	 * Signature: (II)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_HumanPose_loadModel
	(JNIEnv* env, jobject jthis, jint interest, jint rotation) {
		try {
			HumanPose* humanPose = NativeInstance::get<HumanPose>(env, jthis);
			humanPose->loadModel(static_cast<HumanPose::Interest>(interest), static_cast<image::Rotation>(rotation));
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    setInterests
	 * Signature: (I)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_HumanPose_setInterests
	(JNIEnv* env, jobject jthis, jint interests) {
		// TODO choose inference model according to desired aspects
		try {
			HumanPose* humanPose = NativeInstance::get<HumanPose>(env, jthis);
			humanPose->set(static_cast<HumanPose::Interest>(interests));
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    setRotation
	 * Signature: (Lteaselib/core/ai/perception/SceneCapture/Rotation;)Z
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_HumanPose_setRotation
	(JNIEnv* env, jobject jthis, jint value)
	{
		try {
			HumanPose* humanPose = NativeInstance::get<HumanPose>(env, jthis);
			humanPose->set(static_cast<image::Rotation>(value));
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    acquire
	 * Signature: (Lteaselib/core/ai/perception/SceneCapture;Lteaselib/core/ai/perception/SceneCapture/Rotation;)Z
	 */
	JNIEXPORT jboolean JNICALL Java_teaselib_core_ai_perception_HumanPose_acquire
	(JNIEnv* env, jobject jthis, jobject jdevice)
	{
		try {
			Objects::requireNonNull(L"device", jdevice);
			HumanPose* humanPose = NativeInstance::get<HumanPose>(env, jthis);
			VideoCapture* capture = NativeInstance::get<VideoCapture>(env, jdevice);
			return humanPose->acquire(capture);
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
			return false;
		} catch (exception& e) {
			JNIException::rethrow(env, e);
			return false;
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return false;
		} catch (JNIException& e) {
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
			HumanPose* humanPose = NativeInstance::get<HumanPose>(env, jthis);
			JNIByteArray image(env, jimage);
			return humanPose->acquire(image.bytes, image.size);
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
			return false;
		} catch (exception& e) {
			JNIException::rethrow(env, e);
			return false;
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return false;
		} catch (JNIException& e) {
			e.rethrow();
			return false;
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    results
	 * Signature: ()Ljava/util/List;
	 */
	JNIEXPORT jobject JNICALL Java_teaselib_core_ai_perception_HumanPose_estimate
	(JNIEnv* env, jobject jthis, jlong timestamp)
	{
		try {
			HumanPose* humanPose = NativeInstance::get<HumanPose>(env, jthis);
			auto millis = std::chrono::milliseconds(timestamp);
			const vector<Pose> poses = humanPose->estimate(millis);

			vector<jobject> results;
			for_each(poses.begin(), poses.end(), [&env, &results](const Pose& pose) {
				results.push_back(HumanPose::estimation(env, pose));
			});

			return JNIUtilities::asList(env, results);
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
			return nullptr;
		} catch (exception& e) {
			JNIException::rethrow(env, e);
			return nullptr;
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return nullptr;
		} catch (JNIException& e) {
			e.rethrow();
			return nullptr;
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_HumanPose
	 * Method:    dispose
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_HumanPose_dispose
	(JNIEnv* env, jobject jthis)
	{
		try {
			HumanPose* humanPose = NativeInstance::get<HumanPose>(env, jthis);
			delete humanPose;
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

}

HumanPose::HumanPose()
	: interests(0)
	, rotation(image::Rotation::None)
{}

void HumanPose::loadModel(Interest interest, const aifx::image::Rotation rotation_)
{
	model_cache(interest, rotation_);
}

void HumanPose::set(const Interest flags)
{
	this->interests = flags;
}

void HumanPose::set(const image::Rotation value)
{
	this->rotation = value;
}

bool HumanPose::acquire(VideoCapture* capture)
{
	if (capture->started()) {
		*capture >> frame;
		return !frame.empty();
	} else {
		return false;
	}
}

bool HumanPose::acquire(const void* image, int size)
{
	if (size == 0) {
		throw invalid_argument("no bytes");
	} else {
		Mat data(1, size, CV_8UC1, (void*) image);
		imdecode(data, IMREAD_COLOR).copyTo(frame);
		if (frame.empty()) {
			throw invalid_argument("not an image");
		}
		return true;
	}
}

const vector<Pose> HumanPose::estimate(const std::chrono::milliseconds timestamp)
{
	if (frame.empty())  throw logic_error("empty frame");
	if (interests == 0) throw invalid_argument("no interests");
	aifx::pose::Movenet* model = model_cache(interests, rotation, frame.size());
	if (!model) throw logic_error("no model");
	return (*model)(frame, rotation, timestamp);
}

jobject HumanPose::estimation(JNIEnv* env, const aifx::pose::Pose& pose)
{
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
	} else if (isnan(head.x) || isnan(head.y)) {
			jpose = env->NewObject(
				resultClass,
				JNIClass::getMethodID(env, resultClass, "<init>", "(F)V"),
				pose.distance
			);
		} else if (isnan(gaze.x) || isnan(gaze.y) || isnan(gaze.z)) {
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
	return jpose;
}

HumanPose::ModelCache::~ModelCache()
{
	for(auto entry : elements)  { delete entry.second; }
}

aifx::pose::Movenet* HumanPose::ModelCache::operator()(int interests, image::Rotation rotation, const cv::Size& image)
{
	const Movenet::Model model = (interests & (UpperTorso + LowerTorso + LegsAndFeet)) != 0 && (interests & MultiPose) == 0
		? Movenet::Model::SinglePoseExact
		: Movenet::Model::MultiposeFast;
	const bool portait_image = image.width < image.height;
	const bool orientation_change = rotation == aifx::image::Rotation::None || rotation == image::Rotation::Rotate_180;
	const image::Orientation orientation = orientation_change ^ portait_image
		? image::Orientation::Landscape
		: image::Orientation::Portrait;
	const int key = Movenet::resource(model, orientation);
	auto pose_estimation = elements.find(key);
	if (pose_estimation == elements.end()) {
		return elements[key] = new Movenet(model, orientation, TfLiteDelegateV2::GPU_CPU);
	}
	return pose_estimation->second;
}
