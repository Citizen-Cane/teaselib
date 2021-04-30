#include "pch.h"

#include <shlwapi.h>

#include <algorithm>
#include <filesystem>
#include <string>
#include <string_view>
#include <vector>

#include <JNIString.h>
#include <JNIUtilities.h>

#include <UnsupportedLanguageException.h>

#include <Tests/AIfxTestFramework/Blob.h>
#include <Compute/Resource.h>

#include <teaselib_core_ai_deepspeech_DeepSpeechRecognizer.h>
#include "DeepSpeechRecognizer.h"

using namespace aifx::audio;
//using namespace aifx::speech;
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

			char thisModule[MAX_PATH];
			DWORD size = ::GetModuleFileNameA(Resource::getModuleHandle(), thisModule, MAX_PATH);
			if (size == 0) {
				throw NativeException(::GetLastError(), L"model");
			}

			const filesystem::path models = thisModule;
			filesystem::path model = filesystem::path(thisModule).parent_path().append("deepspeech");
			JNIStringUTF8 languageCode(env, jlanguageCode);
			try {
				DeepSpeechRecognizer* speechRecognizer = new DeepSpeechRecognizer(model.string().c_str(), languageCode);
				return reinterpret_cast<jlong>(speechRecognizer);
			} catch (invalid_argument& e) {
				throw e;
			} catch (exception& e) {
				const string what = e.what();
				wstringstream message;
				message << what.substr(0, what.size()).c_str() << ": " << model.append(languageCode.c_str());
				throw UnsupportedLanguageException(E_INVALIDARG, message.str().c_str());
			}
		} catch (invalid_argument& e) {
			JNIException::rethrow(env, e);
			return 0;
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
	 * Class:     teaselib_core_ai_deepspeech_DeepSpeechRecognizer
	 * Method:    setMaxAlternates
	 * Signature: (I)V
	 */
	JNIEXPORT void JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_setMaxAlternates
	(JNIEnv* env, jobject jthis, jint maxAlternates)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			speechRecognizer->setMaxAlternates(maxAlternates);
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
	 * Class:     teaselib_core_ai_perception_DeepSpeechRecognizer
	 * Method:    decode
	 * Signature: ()I
	 */
	JNIEXPORT jint JNICALL Java_teaselib_core_ai_deepspeech_DeepSpeechRecognizer_decode
	(JNIEnv* env, jobject jthis)
	{
		try {
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			const aifx::speech::SpeechAudioStream::Status status = speechRecognizer->decode();
			return static_cast<int>(status);
		} catch (exception& e) {
			JNIException::rethrow(env, e);
			return static_cast<int>(aifx::speech::SpeechAudioStream::Status::Cancelled);
		} catch (NativeException& e) {
			JNIException::rethrow(env, e);
			return static_cast<int>(aifx::speech::SpeechAudioStream::Status::Cancelled);
		} catch (JNIException& e) {
			e.rethrow();
			return static_cast<int>(aifx::speech::SpeechAudioStream::Status::Cancelled);
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
			for_each(phrases.begin(), phrases.end(), [env, &phrases, &all](jobjectArray phrase) {
				vector<string> words = JNIUtilities::stringArray(env, phrase);
				all.insert(words.begin(), words.end());
			});
			speechRecognizer->setHotWords(all);
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
				const aifx::Blob<short> data(speech);
				speechRecognizer->emulate(data, static_cast<unsigned int>(data.size()));
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
		}
		catch (exception& e) {
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

DeepSpeechRecognizer::DeepSpeechRecognizer(const char* path, const char* languageCode)
	: recognizer(path, languageCode)
	, audioStream(recognizer, aifx::speech::SpeechAudioStream::audio_buffer_capacity_default, aifx::speech::VoiceActivationDetection::Mode::Quality, 24, 16, 80)
	, audio(AudioCapture::DeviceInfo::defaultDevice(), recognizer.sample_rate(), aifx::speech::SpeechAudioStream::feed_audio_samples / 2)
	, input([this](const short* audio, unsigned int samples) {
		aifx::speech::SpeechAudioStream::FeedState feed_stste;
		const unsigned int consumed = audioStream.feed(audio, samples, feed_stste);
		if (consumed < samples) {
			if (feed_stste == aifx::speech::SpeechAudioStream::FeedState::FinishDecodeStream) {
				// ok - waiting to finish decode
			} else {
				cerr << endl << "DeepSpeechAudioStream: buffer size insufficient - " << samples - consumed << " samples dropped";
			}
		}
	})
{
	recognizer.enableExternalScorer(true);
}

DeepSpeechRecognizer::~DeepSpeechRecognizer()
{
	stop();
	audioStream.cancel();
}

const string& DeepSpeechRecognizer::languageCode() const
{
	return recognizer.language_code();
}

void DeepSpeechRecognizer::setMaxAlternates(int n)
{
	recognizer.setMaxAlternates(n);
}

void DeepSpeechRecognizer::setHotWords(const set<string>& words)
{
	recognizer.setHotWords(words, aifx::speech::SpeechRecognizer::HotwordBoost::Medium);
}

void DeepSpeechRecognizer::start()
{
	audioStream.reset();
	audio.start(input);
}

void DeepSpeechRecognizer::stop()
{
	audio.stop();
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
	audioStream.emulate(speech);
}

void DeepSpeechRecognizer::emulate(const short* speech, unsigned int samples)
{
	stop();
	audioStream.reset();
	try {
		while (samples) {
			unsigned int consumed;
			aifx::speech::SpeechAudioStream::FeedState feed_state;
			consumed = audioStream.feed(speech, min<unsigned int>(samples, aifx::speech::SpeechAudioStream::vad_frame_size * 5), feed_state);
			samples -= consumed;
			speech += consumed;
			if (samples == 0 || audioStream == aifx::speech::SpeechAudioStream::Status::Done) break; else this_thread::sleep_for(10ms);
		}

		if (audioStream == aifx::speech::SpeechAudioStream::Status::Running) {
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
	audioStream.cancel();
}

aifx::speech::SpeechAudioStream::Status DeepSpeechRecognizer::decode()
{
	return audioStream.decode();
}

const vector<aifx::speech::RecognitionResult> DeepSpeechRecognizer::results() const
{
	return recognizer;
}


const jobject DeepSpeechRecognizer::jresults(JNIEnv* env, const vector<aifx::speech::RecognitionResult>& results)
{
	if (results.empty()) return nullptr;
	if (results.at(0).text.empty()) return nullptr;

	jobject jresults = JNIUtilities::newList(env, results.size());
	const float normalization = results.at(0).confidence;

	jmethodID add = env->GetMethodID(JNIClass::getClass(env, "java/util/List"), "add", "(Ljava/lang/Object;)Z");
	if (env->ExceptionCheck()) throw JNIException(env);
	jclass resultClass = JNIClass::getClass(env, "teaselib/core/ai/deepspeech/DeepSpeechRecognizer$Result");
	if (env->ExceptionCheck()) throw JNIException(env);
	jmethodID init = JNIClass::getMethodID(env, resultClass, "<init>", "(FLjava/util/List;)V");
	if (env->ExceptionCheck()) throw JNIException(env);

	for_each(results.begin(), results.end(), [env, normalization, &resultClass, &add, &init, &jresults](const aifx::speech::RecognitionResult& result) {
		const float confidence = normalization / result.confidence;
		vector<string> words;
		for_each(result.words.begin(), result.words.end(), [env, normalization, &words](const aifx::speech::RecognitionResult::Word& word) {
			words.push_back(word.word);
			});
		jobject jwords = JNIUtilities::asList(env, words);
		jobject jresult = env->NewObject(resultClass, init, confidence, jwords);
		if (env->ExceptionCheck()) throw JNIException(env);
		env->CallObjectMethod(jresults, add, jresult);
		if (env->ExceptionCheck()) throw JNIException(env);
	});

	return jresults;
}
