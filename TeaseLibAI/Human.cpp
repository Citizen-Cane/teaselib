#include "pch.h"

#include<exception>
#include<stdexcept>

#include <JNIArray.h>
#include <JNIObject.h>
#include <JNIUtilities.h>

#include <Pose/Person.h>

#include <teaselib_core_ai_perception_Person.h>
#include "Human.h"
#include "HumanPose.h"

using namespace aifx::video;
using namespace std;

extern "C"
{

	/*
	 * Class:     teaselib_core_ai_perception_Person
	 * Method:    newNativeInstance
	 * Signature: ()J
	 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_perception_Person_newNativeInstance
	(JNIEnv* env, jclass) {
		try {
			Human* human = new Human();
			return reinterpret_cast<jlong>(human);
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
	 * Class:     teaselib_core_ai_perception_Person
	 * Method:    startTracking
	 * Signature: (J)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_Person_startTracking
	(JNIEnv* env, jobject jperson)
	{
		try {
			Human* human = NativeInstance::get<Human>(env, jperson);
			human->startTracking();
		}
		catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
		}
		catch (exception& e) {
			JNIException::rethrow(env, e);
		}
		catch (NativeException& e) {
			JNIException::rethrow(env, e);
		}
		catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_Person
	 * Method:    update
	 * Signature: (Lteaselib/core/ai/perception/Person;Lteaselib/core/ai/perception/HumanPose;Lteaselib/core/ai/perception/SceneCapture;J)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_Person_update__Lteaselib_core_ai_perception_HumanPose_2Lteaselib_core_ai_perception_SceneCapture_2IJ
	(JNIEnv* env, jobject jperson, jobject jhumanPose, jobject jdevice, jint rotation, jlong timestamp)
	{
		try {
			Human* human = NativeInstance::get<Human>(env, jperson);
			HumanPose* humanPose = NativeInstance::get<HumanPose>(env, jhumanPose);
			aifx::video::VideoCapture* device = NativeInstance::get<VideoCapture>(env, jdevice);

			humanPose->set(static_cast<aifx::image::Rotation>(rotation));
			humanPose->acquire(device);
			auto millis = std::chrono::milliseconds(timestamp);
			vector<aifx::pose::Pose> poses = humanPose->estimate(millis);

			if (poses.empty()) {
				Human::update(env, jperson, human->person.estimated(chrono::milliseconds(timestamp)));
			} else {
				human->person.update(poses.at(0));
				Human::update(env, jperson, human->person.pose());
			}
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
	 * Class:     teaselib_core_ai_perception_Person
	 * Method:    update
	 * Signature: (Ljava/util/List;Lteaselib/core/ai/perception/HumanPose;Lteaselib/core/ai/perception/SceneCapture;J)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_Person_update__Ljava_util_List_2Lteaselib_core_ai_perception_HumanPose_2Lteaselib_core_ai_perception_SceneCapture_2IJ
	(JNIEnv* env, jclass, jobject, jobject, jobject, jint, jlong)
	{
		try {
			throw logic_error("not implemented");
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
	 * Class:     teaselib_core_ai_perception_Person
	 * Method:    dispose
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_perception_Person_dispose
	(JNIEnv* env, jobject jthis)
	{
		try {
			Human* human = NativeInstance::get<Human>(env, jthis);
			delete human;
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

void Human::startTracking()
{
	person.clearTimeline();
}

void Human::update(JNIEnv* env, jobject jperson, const aifx::pose::Pose& pose)
{
	jobject jestimation = HumanPose::estimation(env, pose);
	jclass personClass = JNIClass::getClass(env,jperson);
	env->CallVoidMethod(
		jperson,
		JNIClass::getMethodID(
			env,
			personClass,
			"update",
			"(Lteaselib/core/ai/perception/HumanPose$Estimation;)V"),
			jestimation);
	if (env->ExceptionCheck()) throw JNIException(env);
}
