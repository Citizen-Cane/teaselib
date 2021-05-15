#include "stdafx.h"

#include <algorithm>
#include <iostream>
#include <sstream>
#include <stdexcept>

#include <delayimp.h>

#include <JNIUtilities.h>

#include "LoquendoSpeechSynthesizer.h"
#include "LoquendoVoice.h"


using namespace std;

const char* LoquendoSpeechSynthesizer::Sdk = "LTTS7";

HMODULE loadDll(const char* library)
{
    return ::LoadLibraryA(library);
}

const char* LoquendoTTS7 = "C:\\Program Files\\Loquendo\\LTTS7\\bin\\LoqTTS7.dll";

LoquendoSpeechSynthesizer::LoquendoSpeechSynthesizer(JNIEnv* env)
	: SpeechSynthesizer(env)
    , loquendoLibrary(loadDll(LoquendoTTS7))
    , hSession(nullptr), hReader(nullptr), current(OutputType::None)
{
    if (!loquendoLibrary) throw invalid_argument(LoquendoTTS7);

    try {
        checkResult(ttsNewSession(&hSession, NULL));
        checkResult(ttsNewReader(&hReader, hSession));
        checkResult(ttsSetTextFormat(hReader, ttsTextFormatType::TTSAUTODETECTFORMAT));
    } catch (...) {
            dispose();
            throw;
    }
}

LoquendoSpeechSynthesizer::~LoquendoSpeechSynthesizer()
{
    dispose();
}

jobject LoquendoSpeechSynthesizer::enumerate_voices(jobject jttsImpl)
{
    vector<NativeObject*> voices;
    char* id = 0;
    ttsHandleType phEnum = nullptr;
    checkResult(ttsEnumFirst(&phEnum, hSession, ttsQueryType::TTSOBJECTVOICE, nullptr, &id));
    while (id != nullptr) {
        ttsHandleType phVoice = nullptr;
        checkResult(ttsNewVoice(&phVoice, hSession, id));
        string gender = queryVoiceAttribute(id, "Gender");

        ttsHandleType phLanguage = nullptr;
        string language = queryVoiceAttribute(id, "MotherTongue");
        checkResult(ttsNewLanguage(&phLanguage, hSession, language.c_str()));

        string languageAliases = queryLanguageAttribute(language.c_str(), "Language");
        string locale = extractLocale(languageAliases);

        const char whitespace = ' ';
        const string languageDescription = queryLanguageAttribute(language.c_str(), "Description");
        const vector<string> words = teaselib::strings::split(languageDescription, whitespace);
        auto begin = words.begin();
        auto end = words.end() - 1;
        const string languageName = !words.empty() &&  end->find("anguage") != string::npos
            ?  teaselib::strings::join(begin, end, whitespace) : languageDescription;

        voices.push_back(new LoquendoVoice(env, jttsImpl, phVoice, phLanguage, id, gender.c_str(), locale.c_str(), languageName.c_str()));
        checkResult(ttsEnumNext(phEnum, &id));
    }
    checkResult(ttsEnumClose(phEnum));
    return JNIUtilities::asList(env, voices);
}


void LoquendoSpeechSynthesizer::addLexiconEntry(const wchar_t* const /*locale*/, const wchar_t* const /*word*/, const SPPARTOFSPEECH /*partOfSpeech*/, const wchar_t* const /*pronunciation*/)
{
    // Can only load complete dictionaries, plus entries with phonemes distort the prosody
    // TODO review ssml tests and relevance on SAPI side -> remove custom impl  dictionaires in favor for java solution
}

void LoquendoSpeechSynthesizer::setVoice(const Voice* voice)
{
    checkResult(static_cast<const LoquendoVoice*>(voice)->applyTo(hReader));
}

void LoquendoSpeechSynthesizer::speak(const wchar_t* prompt)
{
    setOutput(OutputType::AudioDevice);
    applyHints();
    speak(teaselib::strings::utf8(prompt), ttsTRUE);
    awaitDone();
}

std::wstring LoquendoSpeechSynthesizer::speak(const wchar_t* prompt, const wchar_t* path)
{
    const string utf = teaselib::strings::utf8(path);
    setOutput(OutputType::File, utf.c_str());
    applyHints();
    speak(teaselib::strings::utf8(prompt), ttsFALSE);
    return path;
}

void LoquendoSpeechSynthesizer::speak(const string& prompt, ttsBoolType asynchronous) {
    checkResult(ttsRead(hReader, prompt.c_str(), asynchronous, ttsFALSE, nullptr));
}

void LoquendoSpeechSynthesizer::awaitDone() {
    ttsBoolType signalled = 0;
    while (!signalled) {
        if (cancelSpeech) {
            checkResult(ttsStop(hReader));
            break;
        } else {
            checkResult(ttsWaitForEndOfSpeech(hReader, 100, &signalled));
        }
    }
}

void LoquendoSpeechSynthesizer::stop()
{
    cancelSpeech = true;
}

void LoquendoSpeechSynthesizer::setOutput(OutputType output, const char* wav)
{
    if (output == OutputType::AudioDevice && output == current) {
        return;
    } else {
        char* destination;
        if (output == OutputType::AudioDevice) {
            destination = "LTTS7AudioBoard";
        } else if (output == OutputType::File) {
            destination = "LTTS7AudioFile";
        } else {
            throw invalid_argument("output");
        }
        checkResult(ttsSetAudio(hReader, destination, wav, 32000, tts_LINEAR, tts_MONO, nullptr));
    }
    current = output;
}

void LoquendoSpeechSynthesizer::applyHints()
{
    auto reading_mood = std::find_if(hints.begin(), hints.end(), [](const wstring& hint) {
        return hint == L"<mood=reading>";
    });

    if (reading_mood != hints.end()) {
        checkResult(ttsSetVolume(hReader, 15));
        checkResult(ttsSetSpeed(hReader, 40));
    } else {
        checkResult(ttsSetVolume(hReader, 12));
        checkResult(ttsSetSpeed(hReader, 50));
    }
}

string LoquendoSpeechSynthesizer::extractLocale(const string& languageAliases) {
    const vector<string> aliases = teaselib::strings::split(languageAliases, ',');

    auto locale = find_if(aliases.begin(), aliases.end(), [](const string& alias) ->bool {
        return alias.find("-") != string::npos;
    });

    if (locale == aliases.end()) {
        locale = find_if(aliases.begin(), aliases.end(), [](const string& alias) ->bool {
            return alias.length() == 2;
        });
    }

    if (locale == aliases.end()) {
        return "??-??";
    } else {
        return *locale;
    }
}

string LoquendoSpeechSynthesizer::queryVoiceAttribute(const char* id, const char* attribute) {
    auto value = std::unique_ptr<const char>(queryAttribute(ttsQueryType::TTSOBJECTVOICE, id, attribute));
    return value.get();
}

string LoquendoSpeechSynthesizer::queryLanguageAttribute(const char* id, const char* attribute) {
    auto value = std::unique_ptr<const char>(queryAttribute(ttsQueryType::TTSOBJECTLANGUAGE, id, attribute));
    return value.get();
}

const char* LoquendoSpeechSynthesizer::queryAttribute(ttsQueryType queryType, const char* id, const char* attribute) {
    char* pResultBuffer = new char[ttsSTRINGMAXLEN];
    checkResult(ttsQueryAttribute(hSession, queryType, attribute, id, pResultBuffer, ttsSTRINGMAXLEN));
    return pResultBuffer;
}

void LoquendoSpeechSynthesizer::checkResult(ttsResultType r)
{
    if (r != tts_OK) {
        throw runtime_error(ttsGetErrorMessage(r));
    }
}

void LoquendoSpeechSynthesizer::dispose()
{
    if (hReader) ttsDeleteReader(hReader);
    if (hSession) ttsDeleteSession(hSession);
    if (loquendoLibrary) FreeLibrary(loquendoLibrary);
}
