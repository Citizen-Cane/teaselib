#include "pch.h"

#include <shlwapi.h>

#include <algorithm>
#include <string>
#include <vector>

#include <JNIString.h>
#include <JNIUtilities.h>

#include <Blob.h>
#include <AIfxDeepSpeechAudioStream.h>

#include <teaselib_core_ai_deepspeech_DeepSpeechRecognizer.h>
#include "DeepSpeechRecognizer.h"

using namespace aifx;
using namespace std;

extern "C"
{

	/*
	* Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	* Method:    init
	* Signature: (Ljava/lang/String;)J
	*/
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_init
	(JNIEnv* env, jclass, jstring jlanguageCode)
	{
		try {
			Objects::requireNonNull(L"locale", jlanguageCode);

			JNIStringUTF8 languageCode(env, jlanguageCode);
			char path[MAX_PATH];
			GetModuleFileNameA(nullptr, path, MAX_PATH);
			PathRemoveFileSpecA(path);
			DeepSpeechRecognizer* speechRecognizer = new DeepSpeechRecognizer(path, languageCode);
			return reinterpret_cast<jlong>(speechRecognizer);
		} catch(std::exception& e) {
			JNIException::rethrow(env, e);
			return 0;
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return 0;
		} catch (JNIException& e) {
			e.rethrow();
			return 0;
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    languageCode
	 * Signature: ()Ljava/lang/String;
	 */
	JNIEXPORT jstring JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_languageCode
	(JNIEnv* env, jobject jthis)
	{
		try {
			const DeepSpeechRecognizer* speechRecognizer = NativeObject::get<DeepSpeechRecognizer>(env, jthis);
			return JNIStringUTF8(env, speechRecognizer->languageCode());
		} catch (std::exception& e) {
			JNIException::rethrow(env, e);
			return 0;
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return nullptr;
		} catch (JNIException& e) {
			e.rethrow();
			return nullptr;
		}
	}


	/*
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    decode
	 * Signature: ()I
	 */
	JNIEXPORT jint JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_decode
	(JNIEnv* env, jobject jthis)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeObject::get<DeepSpeechRecognizer>(env, jthis);
			const DeepSpeechAudioStream::Status status = speechRecognizer->decode();
			return static_cast<int>(DeepSpeechAudioStream::Status::Idle);
		} catch (std::exception& e) {
			JNIException::rethrow(env, e);
			return static_cast<int>(DeepSpeechAudioStream::Status::Cancelled);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return static_cast<int>(DeepSpeechAudioStream::Status::Cancelled);
		} catch (JNIException& e) {
			e.rethrow();
			return static_cast<int>(DeepSpeechAudioStream::Status::Cancelled);
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    results
	 * Signature: ()Lteaselib/core/ai/perception/DeepSpeechRecognizer/Results;
	 */
	JNIEXPORT jobject JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_results
	(JNIEnv* env, jobject jthis)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeObject::get<DeepSpeechRecognizer>(env, jthis);
			vector<DeepSpeech::Recognition> results = speechRecognizer->results();
			jobject jresults = JNIUtilities::newList(env, results.size());
			const float normalization = results.at(0).confidence;

			jmethodID add = env->GetMethodID(JNIClass::getClass(env, "java/util/ArrayList"), "add", "(Ljava/lang/Object;)Z");
			if (env->ExceptionCheck()) throw JNIException(env);
			jclass resultClass = JNIClass::getClass(env, "teaselib/core/ai/deepspeech/DeepSpeechRecognizer$Result");
			if (env->ExceptionCheck()) throw JNIException(env);
			jmethodID init = JNIClass::getMethodID(env, resultClass, "<init>", "(FLjava/util/ArrayList;)V");
			if (env->ExceptionCheck()) throw JNIException(env);

			for_each(results.begin(), results.end(), [env, normalization, &resultClass, &add, &init, &jresults](const DeepSpeech::Recognition& result) {
				const float confidence = normalization / result.confidence;
				vector<string> words;
				for_each(result.words.begin(), result.words.end(), [env, normalization, &words](const DeepSpeech::meta_word& word) {
					words.push_back(word.word);
				});
				jobject jwords = JNIUtilities::asList(env, words);
				jobject jresult = env->NewObject(resultClass, init,	confidence,	jwords);
				if (env->ExceptionCheck()) throw JNIException(env);
				env->CallObjectMethod(jresults,	add, jresult);
				if (env->ExceptionCheck()) throw JNIException(env);
			});
			return jresults;
		} catch (std::exception& e) {
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
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    startRecognition
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_startRecognition
	(JNIEnv* env, jobject jthis)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeObject::get<DeepSpeechRecognizer>(env, jthis);
			speechRecognizer->start();
		} catch (std::exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    emulateRecognition
	 * Signature: (Ljava/lang/String;)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_emulateRecognition
	(JNIEnv* env, jobject jthis, jstring jspeech)
	{
		try {
			Objects::requireNonNull(L"speech", jspeech);
			DeepSpeechRecognizer* speechRecognizer = NativeObject::get<DeepSpeechRecognizer>(env, jthis);
			JNIStringUTF8 speech(env, jspeech);
			bool file = ::PathFileExistsA(speech);
			if (file) {
				aifx::Blob<short> data(speech);
			} else {
				speechRecognizer->emulate(speech);
			}
		} catch (std::exception& e) {
			JNIException::rethrow(env, e);			
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    stopRecognition
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_stopRecognition
	(JNIEnv* env, jobject jthis)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeObject::get<DeepSpeechRecognizer>(env, jthis);
			speechRecognizer->stop();
		} catch (std::exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}


	/*
		* Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
		* Method:    dispose
		* Signature: ()V
		*/
	JNIEXPORT void JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_dispose
	(JNIEnv* env, jobject jthis)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeObject::get<DeepSpeechRecognizer>(env, jthis);
			speechRecognizer->stop();
			NativeObject::dispose(env, jthis, speechRecognizer);
		} catch (std::exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

}

DeepSpeechRecognizer::DeepSpeechRecognizer(const char* path, const char* languageCode)
	: recognizer(path, languageCode)
	, audio(aifx::AudioCapture::DeviceInfo::defaultDevice(), recognizer.sample_rate(), aifx::DeepSpeechAudioStream::feed_audio_samples / 2)
	, input([this](const short* audio, unsigned int samples) {
		const unsigned int consumed = recognizer.feed(audio, samples);
		if (consumed < samples) {
			if (recognizer == DeepSpeechAudioStream::FeedState::FinishDecodeStream) {
				// ok
			} else {
				cerr << endl << "DeepSpeechAudioStream: buffer size insufficient - " << samples - consumed << " samples dropped";
			}
		}
	})
{}

DeepSpeechRecognizer::~DeepSpeechRecognizer()
{
	recognizer.cancel();
}

const string& DeepSpeechRecognizer::languageCode() const
{
	return recognizer.language_code();
}

void DeepSpeechRecognizer::start()
{
	audio.start(input);
}

void DeepSpeechRecognizer::stop()
{
	audio.stop();
}

void DeepSpeechRecognizer::emulate(const char* speech)
{
	audio.stop();
}

void DeepSpeechRecognizer::emulate(const short* speech, unsigned int samples)
{
	audio.stop();
	recognizer.clear();
	std::thread audio_in([this, &speech, &samples]() {
		try {
			while (samples >= DeepSpeechAudioStream::vad_frame_size) {
				unsigned int consumed;
				while (0 == (consumed = recognizer.feed(speech, DeepSpeechAudioStream::vad_frame_size))) {
					if (recognizer == DeepSpeechAudioStream::Status::Done) return;
					this_thread::sleep_for(100ms);
				}
				samples -= consumed;
				speech += consumed;
			}
			return;
		} catch (std::exception& e) {
			recognizer.cancel();
			cerr << e.what();
			throw e;
		}
	});
}

DeepSpeechAudioStream::Status DeepSpeechRecognizer::decode()
{
	return recognizer.decode(100);
}

const vector<DeepSpeech::Recognition> DeepSpeechRecognizer::results() const
{
	return recognizer;
}
