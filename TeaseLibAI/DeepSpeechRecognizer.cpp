#include "pch.h"

#include <shlwapi.h>

#include <algorithm>
#include <filesystem>
#include <ranges>
#include <string>
#include <string_view>
#include <vector>

#include <JNIString.h>
#include <JNIUtilities.h>

#include <UnsupportedLanguageException.h>

#include <Tests/AIfxTestFramework/Blob.h>

#include <Audio/WavFile.h>
#include <Compute/AllocationPlan.h>
#include <Compute/Devices.h>
#include <Compute/Model.h>
#include <Compute/ModelZoo.h>
#include <Compute/Resource.h>
#include <Whisper/Silero.h>
#include <Whisper/Whisper.h>
#include <Whisper/WhisperHypothesizingContext.h>

#include <teaselib_core_ai_deepspeech_DeepSpeechRecognizer.h>
#include "DeepSpeechRecognizer.h"

using namespace aifx::audio;
using namespace aifx::compute;
using namespace aifx::speech;
using namespace aifx::util;
using namespace std;

extern "C"
{

	/*
	 * Class:     teaselib_core_ai_deepspeech_DeepSpeechRecognizer
	 * Method:    init
	 * Signature: (Ljava/lang/String;)J
	 */
	JNIEXPORT jlong JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_newNativeInstance
	(JNIEnv* env, jclass, jstring jlanguageCode)
	{
		try {
			Objects::requireNonNull(L"locale", jlanguageCode);
			ModelZoo models = aifx::util::Resource::getModuleDirectory() / "models";
			models.emplace_back(WhisperHypothesizingContext::model_set());
			models.emplace_back(vad::Silero::model_set);
			auto plan = AllocationPlan(Devices::all(), models);
			if (!plan.allocate()) throw bad_alloc();

			JNIStringUTF8 languageCode(env, jlanguageCode);
			try {
				DeepSpeechRecognizer* speechRecognizer = new DeepSpeechRecognizer(plan, languageCode);
				return reinterpret_cast<jlong>(speechRecognizer);
			} catch (invalid_argument& e) {
				const string what = e.what();
				if (!what.contains(languageCode)) {
					throw e;
				}
				wstringstream message;
				message << e.what();
				throw UnsupportedLanguageException(E_INVALIDARG, message.str());
			}
		} catch(exception& e) {
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
			const DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			return JNIStringUTF8(env, speechRecognizer->languageCode());
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
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    decode
	 * Signature: ()I
	 */
	JNIEXPORT jint JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_decode
	(JNIEnv* env, jobject jthis)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			const SpeechAudioStream::Status status = speechRecognizer->decode();
			return static_cast<int>(status);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
			return static_cast<int>(SpeechAudioStream::Status::Idle);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return static_cast<int>(SpeechAudioStream::Status::Idle);
		} catch (JNIException& e) {
			e.rethrow();
			return static_cast<int>(SpeechAudioStream::Status::Idle);
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
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			return DeepSpeechRecognizer::jresults(env, speechRecognizer->results());
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
	 * Class:     teaselib_core_ai_deepspeech_DeepSpeechRecognizer
	 * Method:    setChoices
	 * Signature: (Ljava/util/List;)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_setChoices
	(JNIEnv* env, jobject jthis, jobject jphrases)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			vector<jobjectArray> phrases = JNIUtilities::objectArrays(env, jphrases);
			set<string> all;
			vector<string> phrases_list(phrases.size());
			ranges::for_each(phrases, [env, &all, &phrases_list] (jobjectArray jphrase) {
				const vector<string> words = JNIUtilities::stringArray(env, jphrase);
				all.insert(words.begin(), words.end());
				string phrase = aifx::text::join(words);
				phrases_list.emplace_back(phrase);
			});
			speechRecognizer->setHotWords(all, phrases_list);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
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
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			speechRecognizer->start();
		} catch (exception& e) {
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
	JNIEXPORT void JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_emulate
	(JNIEnv* env, jobject jthis, jstring jspeech)
	{
		try {
			Objects::requireNonNull(L"speech", jspeech);
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			JNIStringUTF8 speech(env, jspeech);
			if (PathFileExistsA(speech)) {
				speechRecognizer->emulate(aifx::audio::WavFile(speech));
			} else {
				speechRecognizer->emulate(speech);
			}
		} catch (exception& e) {
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
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			speechRecognizer->stop();
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

	/*
	 * Class:     teaselib_core_ai_deepspeech_DeepSpeechRecognizer
	 * Method:    interruptNativeEventHandler
	 * Signature: ()V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_stopEventLoop
	(JNIEnv* env, jobject jthis)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			speechRecognizer->stopEventLoop();
		} catch (exception& e) {
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
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			NativeInstance::dispose(env, jthis, speechRecognizer);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
		} catch (JNIException& e) {
			e.rethrow();
		}
	}

}

DeepSpeechRecognizer::DeepSpeechRecognizer(const AllocationPlan& plan, const char* languageCode)
	: recognizer(plan.make<WhisperHypothesizingContext>(languageCode))
	, vad(plan.make<vad::Silero>())
	, audioStream(recognizer, vad)
	, audio(AudioCapture::Devices().default_device, recognizer.sample_rate(), vad.frame_size() * 10)
	, input([this] (const float* audio, unsigned int samples) {
		SpeechAudioStream::FeedState feed_stste;
		const unsigned int consumed = audioStream.feed(audio, samples, feed_stste);
		if (consumed < samples) {
			if (feed_stste == SpeechAudioStream::FeedState::FinishDecodeStream) {
				// ok - waiting to finish decode
			} else {
				cerr << "DeepSpeechAudioStream feed buffer size insufficient - " << samples - consumed << " samples dropped." << endl;
			}
		}
	})
{}

DeepSpeechRecognizer::~DeepSpeechRecognizer()
{
	stop();
	audioStream.cancel();
}

const string& DeepSpeechRecognizer::languageCode() const
{
	return recognizer.lang;
}

void DeepSpeechRecognizer::setHotWords(const set<string>& words, const vector<string>& phrases)
{
	recognizer.set_hotwords(words, aifx::speech::Hotwords::Boost::Medium);
	recognizer.set_phrases(phrases, aifx::speech::Hotwords::Boost::Medium);
}

void DeepSpeechRecognizer::start()
{
	audioStream.reset();
	audio.start(input);
}

void DeepSpeechRecognizer::stop()
{
	audio.stop();
	audioStream.cancel();
}

static auto const isSpace = [](char c){ return c == ' '; };

template <typename OutputIterator> void split(string const& s, OutputIterator out)
{
    auto index = begin(s);
    while (index != end(s)) {
        auto const start_pos = find_if_not(index, end(s), isSpace);
        auto const end_pos = find_if(start_pos, end(s), isSpace);
        if (start_pos != end_pos) {
            *out = string_view(&*start_pos, distance(start_pos, end_pos));
            ++out;
        }
        index = end_pos;
    }
}

void DeepSpeechRecognizer::emulate(const char* speech)
{
	stop();
	audioStream.reset();
	audioStream.emulate(speech);
}

void DeepSpeechRecognizer::emulate(const std::vector<float>& speech)
{
	emulate(speech.data(), static_cast<unsigned int>(speech.size()));
}

void DeepSpeechRecognizer::emulate(const float* speech, unsigned int samples)
{
	stop();
	audioStream.reset();
	try {
		while (samples) {
			unsigned int consumed;
			SpeechAudioStream::FeedState feed_state;
			consumed = audioStream.feed(speech, min<unsigned int>(samples, vad.frame_size() * 10), feed_state);
			samples -= consumed;
			speech += consumed;
			if (samples == 0 || audioStream == SpeechAudioStream::Status::Done) break; else this_thread::sleep_for(100ms);
		}

		if (audioStream == SpeechAudioStream::Status::Running) {
			audioStream.finish();
		}
	} catch (exception& e) {
		audioStream.cancel();
		throw e;
	}
}

void DeepSpeechRecognizer::stopEventLoop()
{
	stop();
}

SpeechAudioStream::Status DeepSpeechRecognizer::decode()
{
	return audioStream.decode();
}

const vector<RecognitionResult> DeepSpeechRecognizer::results() const
{
	return { audioStream };
}


const jobject DeepSpeechRecognizer::jresults(JNIEnv* env, const vector<RecognitionResult>& results)
{
	if (results.empty()) return nullptr;
	if (results.at(0).text.empty()) return nullptr;

	jobject jresults = JNIUtilities::newList(env, results.size());
	const float normalization = results.at(0).words.confidence();

	jmethodID add = env->GetMethodID(JNIClass::getClass(env, "java/util/List"), "add", "(Ljava/lang/Object;)Z");
	if (env->ExceptionCheck()) throw JNIException(env);
	jclass resultClass = JNIClass::getClass(env, "teaselib/core/ai/deepspeech/DeepSpeechRecognizer$Result");
	if (env->ExceptionCheck()) throw JNIException(env);
	jmethodID init = JNIClass::getMethodID(env, resultClass, "<init>", "(FLjava/util/List;)V");
	if (env->ExceptionCheck()) throw JNIException(env);

	ranges::for_each(results, [env, normalization, &resultClass, &add, &init, &jresults] (const RecognitionResult& result) {
		const float confidence = normalization / result.words.confidence();
		jobject jwords = JNIUtilities::asList(env, result.words);
		jobject jresult = env->NewObject(resultClass, init, confidence, jwords);
		if (env->ExceptionCheck()) throw JNIException(env);
		env->CallObjectMethod(jresults, add, jresult);
		if (env->ExceptionCheck()) throw JNIException(env);
	});

	return jresults;
}
