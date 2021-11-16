#include "pch.h"

#include<JNIObject.h>
#include<JNIUtilities.h>
#include<NativeObject.h>

#include <Compute/ComputeContext.h>

#include <teaselib_core_ai_ComputeContext.h>

#include "ComputeContext.h"

using namespace std;

extern "C"
{
	/*
 * Class:     teaselib_core_ai_ComputeContext
 * Method:    newInstance
 * Signature: ()Lteaselib/core/ai/ComputeContext;
 */
	JNIEXPORT jobject JNICALL Java_teaselib_core_ai_ComputeContext_newInstance
	(JNIEnv* env, jclass jclazz)
	{
		try {
			auto context = new aifx::compute::ComputeContext;
			return env->NewObject(jclazz,
				JNIClass::getMethodID(env, jclazz,"<init>",
				"(JZ)V"),
				context,
				context->context != nullptr);
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
	 * Class:     teaselib_core_ai_ComputeContext
	 * Method:    dispose
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_ComputeContext_dispose
	(JNIEnv* env, jobject jthis)
	{
		try {
			aifx::compute::ComputeContext* context = NativeInstance::get<aifx::compute::ComputeContext>(env, jthis);
			delete context;
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

}