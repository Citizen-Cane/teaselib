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

#include <Blob.h>
#include <AIfxResource.h>
#include <AIfxDeepSpeechAudioStream.h>

#include <teaselib_core_ai_deepspeech_DeepSpeechRecognizer.h>
#include "DeepSpeechRecognizer.h"

using namespace aifx;
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
			DWORD size = ::GetModuleFileNameA(aifx::util::Resource::getModuleHandle(), thisModule, MAX_PATH);
			if (size == 0) {
				throw NativeException(::GetLastError(), L"model");
			}

			const filesystem::path models = thisModule;
			filesystem::path model = filesystem::path(thisModule).parent_path().append("deepspeech");
			JNIStringUTF8 languageCode(env, jlanguageCode);
			try {
				DeepSpeechRecognizer* speechRecognizer = new DeepSpeechRecognizer(model.string().c_str(), languageCode);
				return reinterpret_cast<jlong>(speechRecognizer);
			} catch (exception& e) {
				const string what = e.what();
				wstringstream message;
				message << what.substr(0, what.size() -1).c_str() << ": " << model.append(languageCode.c_str());
				throw UnsupportedLanguageException(E_INVALIDARG, message.str().c_str());
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
			DeepSpeechRecognizer* speechRecognizer = NativeInstance::get<DeepSpeechRecognizer>(env, jthis);
			const DeepSpeechAudioStream::Status status = speechRecognizer->decode();
			return static_cast<int>(status);
		} catch (exception& e) {
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
	, audio(aifx::AudioCapture::DeviceInfo::defaultDevice(), recognizer.sample_rate(), aifx::DeepSpeechAudioStream::feed_audio_samples / 2)
	, input([this](const short* audio, unsigned int samples) {
		DeepSpeechAudioStream::FeedState feed_stste;
		const unsigned int consumed = recognizer.feed(audio, samples, feed_stste);
		if (consumed < samples) {
			if (feed_stste == DeepSpeechAudioStream::FeedState::FinishDecodeStream) {
				// ok - waiting to finish decode
			} else {
				cerr << endl << "DeepSpeechAudioStream: buffer size insufficient - " << samples - consumed << " samples dropped";
			}
		}
	})
{
	recognizer.enableExternalScorer();
}

DeepSpeechRecognizer::~DeepSpeechRecognizer()
{
	stop();
	recognizer.cancel();
}

const string& DeepSpeechRecognizer::languageCode() const
{
	return recognizer.language_code();
}

void DeepSpeechRecognizer::start()
{
	recognizer.clear();
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

	vector<DeepSpeech::Recognition> results;
	float confidence = -1.0f;

	istringstream lines(speech);
	string emulated_speech;
	while (getline(lines, emulated_speech))
	{
		vector<string_view> words;
		split(emulated_speech, back_inserter(words));

		vector<DeepSpeech::meta_word> meta_words;
		float timestamp = 0.0f;
		for_each(words.begin(), words.end(), [&meta_words, &timestamp](const string_view& word) {
			float duration = word.size() * 0.1f;
			meta_words.push_back({ string(word), timestamp, duration });
			timestamp += duration + 0.2f;
		});

		results.push_back(
			DeepSpeech::Recognition(
				speech,
				meta_words,
				confidence
			)
		);
		confidence *= 1.05f;
	}

	recognizer.emulate(results);
}

void DeepSpeechRecognizer::emulate(const short* speech, unsigned int samples)
{
	stop();
	try {
		while (samples) {
			DeepSpeechAudioStream::FeedState feed_state;
			unsigned int consumed;
			while (0 == (consumed = recognizer.feed(speech, std::min<unsigned int>(samples, DeepSpeechAudioStream::vad_frame_size), feed_state)))
			{
				if (feed_state == DeepSpeechAudioStream::FeedState::FinishDecodeStream) return; else this_thread::sleep_for(100ms);
			}
			samples -= consumed;
			speech += consumed;
		}

		if (recognizer != DeepSpeechAudioStream::Status::Done) {
			recognizer.finish();
		}

		return;
	} catch (exception& e) {
		recognizer.cancel();
		throw e;
	}
}

void DeepSpeechRecognizer::stopEventLoop()
{
	stop();
	recognizer.cancel();
}

DeepSpeechAudioStream::Status DeepSpeechRecognizer::decode()
{
	return recognizer.decode();
}

const vector<DeepSpeech::Recognition> DeepSpeechRecognizer::results() const
{
	return recognizer;
}


const jobject DeepSpeechRecognizer::jresults(JNIEnv* env, const std::vector<aifx::DeepSpeech::Recognition>& results)
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

	for_each(results.begin(), results.end(), [env, normalization, &resultClass, &add, &init, &jresults](const DeepSpeech::Recognition& result) {
		const float confidence = normalization / result.confidence;
		vector<string> words;
		for_each(result.words.begin(), result.words.end(), [env, normalization, &words](const DeepSpeech::meta_word& word) {
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
