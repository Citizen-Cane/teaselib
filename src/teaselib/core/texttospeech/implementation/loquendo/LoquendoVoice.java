package teaselib.core.texttospeech.implementation.loquendo;

import org.bridj.Pointer;

import teaselib.Sexuality.Gender;
import teaselib.core.texttospeech.TextToSpeechImplementation;
import teaselib.core.texttospeech.Voice;
import teaselib.core.texttospeech.VoiceInfo;

public class LoquendoVoice implements Voice {
    private final LoquendoTTS tts;
    final Pointer<?> hVoice;
    final Pointer<?> hLanguage;

    private final String guid;
    private final Gender gender;
    private final String locale;

    private final VoiceInfo info;

    public LoquendoVoice(LoquendoTTS tts, Pointer<?> hVoice, Pointer<?> hLanguage, String id) {
        this.tts = tts;
        this.hVoice = hVoice;
        this.hLanguage = hLanguage;

        this.guid = tts.sdkName() + "_" + id;
        this.gender = gender(tts.queryVoiceAttribute(Pointer.pointerToCString(id), "Gender"));
        String language = tts.queryVoiceAttribute(Pointer.pointerToCString(id), "MotherTongue");

        String languageAliases = tts.queryLanguageAttribute(Pointer.pointerToCString(language), "Language");
        this.locale = extractLocale(languageAliases);

        String languageDescription = tts.queryLanguageAttribute(Pointer.pointerToCString(language), "Description");
        int removeUnwantedTextIndex = languageDescription.lastIndexOf(" language");
        if (removeUnwantedTextIndex >= 0) {
            languageDescription = languageDescription.substring(0, removeUnwantedTextIndex);
        }
        this.info = new VoiceInfo("Loquendo", languageDescription, id);
    }

    public static String extractLocale(String languageAliases) {
        String locale = null;

        String[] candidates = languageAliases.split(",");
        locale = extractIETFLanguageTag(locale, candidates);

        if (locale == null) {
            locale = extractLanguageCode(candidates);
        }

        if (locale == null) {
            locale = "??-??";
        }

        return locale;
    }

    public static String extractLanguageCode(String[] candidates) {
        String languageCode = null;

        for (String candidate : candidates) {
            if (candidate.length() == 2) {
                languageCode = candidate;
            }
        }
        return languageCode;
    }

    public static String extractIETFLanguageTag(String locale, String[] candidates) {
        for (String candidate : candidates) {
            if (candidate.length() > 2 && candidate.substring(2, 3).equals("-")) {
                locale = candidate;
            }
        }
        return locale;
    }

    private static Gender gender(String gender) {
        if ("male".equalsIgnoreCase(gender)) {
            return Voice.Male;
        } else if ("female".equalsIgnoreCase(gender)) {
            return Voice.Female;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public TextToSpeechImplementation tts() {
        return tts;
    }

    @Override
    public String guid() {
        return guid;
    }

    @Override
    public Gender gender() {
        return gender;
    }

    @Override
    public String locale() {
        return locale;
    }

    @Override
    public VoiceInfo info() {
        return info;
    }

    @Override
    public String toString() {
        return TextToSpeechImplementation.toString(this);
    }
}
