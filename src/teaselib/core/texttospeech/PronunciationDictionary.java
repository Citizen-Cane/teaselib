package teaselib.core.texttospeech;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

import teaselib.core.util.WildcardPattern;

public class PronunciationDictionary {
    final File rootDirectory;
    final Map<Voice, Map<String, String>> cache = new HashMap<>();

    public static PronunciationDictionary empty() {
        return new PronunciationDictionary(null);
    }

    public PronunciationDictionary(File root) {
        super();
        this.rootDirectory = root;
    }

    public Map<String, String> pronounciations(Voice voice) throws IOException {
        if (cache.containsKey(voice)) {
            return cache.get(voice);
        } else {
            Map<String, String> pronounciations = pronunciations(voice.api, voice.vendor, voice.locale, voice.guid);
            cache.put(voice, pronounciations);
            return pronounciations;
        }
    }

    public Map<String, Map<String, String>> pronunciations(String sdkName, String phonemeFormat) throws IOException {
        Map<String, File> languageDictionaries = joinSDKPhonecticDictionaries(sdkName, phonemeFormat);
        return dictionariesByLanguage(languageDictionaries);
    }

    private Map<String, Map<String, String>> dictionariesByLanguage(Map<String, File> languageDictionaries)
            throws IOException, FileNotFoundException {
        Map<String, Map<String, String>> phoneticDictionary = new HashMap<>();
        for (Entry<String, File> languageDictionary : languageDictionaries.entrySet()) {
            Map<String, String> entries = new HashMap<>();
            readProperties(entries, languageDictionary.getValue());
            phoneticDictionary.put(languageDictionary.getKey(), entries);
        }
        return phoneticDictionary;
    }

    private Map<String, File> joinSDKPhonecticDictionaries(String sdkName, String phonemeFormat) {
        String tailingFilename = "." + phonemeFormat + ".properties";
        String wildcard = "*" + tailingFilename;
        File[] dictionaries = listSDKPhonecticDictionaries(sdkName, wildcard);

        Map<String, File> languageDictionaries = new HashMap<>();
        for (File file : dictionaries) {
            String fileName = file.getName();
            String languageTag = fileName.substring(0, fileName.indexOf(tailingFilename));
            languageDictionaries.put(languageTag, file);
        }
        return languageDictionaries;
    }

    private File[] listSDKPhonecticDictionaries(String sdkName, String wildcard) {
        File sdk = new File(rootDirectory, sdkName);
        Pattern pattern = WildcardPattern.compile(wildcard);
        File[] dictionaries = sdk.listFiles((File dir, String name) -> {
            return pattern.matcher(name).matches();
        });
        if (dictionaries == null) {
            dictionaries = new File[] {};
        }
        return dictionaries;
    }

    Map<String, String> pronunciations(String api, String vendor, String locale, String voiceGuid) throws IOException {
        return pronunciations(dictionaries(api, vendor, locale, voiceGuid));
    }

    private Map<String, String> pronunciations(List<File> dictionaries) throws IOException {
        Map<String, String> all = new HashMap<>();
        for (File file : dictionaries) {
            if (file.exists()) {
                readProperties(all, file);
            }
        }

        return all;
    }

    private void readProperties(Map<String, String> all, File file) throws IOException, FileNotFoundException {
        Properties p = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            p.load(fis);
            for (Entry<Object, Object> entry : p.entrySet()) {
                String key = (String) entry.getKey();
                all.put(key, (String) entry.getValue());
            }
        }
    }

    private List<File> dictionaries(String api, String vendor, String locale, String voiceGuid) {
        if (rootDirectory != null) {
            return Arrays.asList(path(language(locale)), path(locale), path(api, language(locale)), path(api, locale),
                    path(api, vendor, language(locale)), path(api, vendor, locale), path(api, vendor, voiceGuid));
        } else {
            return Collections.emptyList();
        }
    }

    private File path(String... components) {
        File file = rootDirectory;
        for (String baseName : components) {
            file = new File(file, baseName);
        }
        return new File(file.getAbsolutePath() + ".properties");
    }

    private String language(String locale) {
        return locale.substring(0, 2);
    }

    public String correct(Voice voice, String prompt) throws IOException {
        Map<String, String> pronounciations = pronounciations(voice);
        String correctedPrompt = prompt;
        for (Entry<String, String> entry : pronounciations.entrySet()) {
            String regex = "\\b" + entry.getKey() + "\\b";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            correctedPrompt = pattern.matcher(correctedPrompt).replaceAll(entry.getValue());
        }
        return correctedPrompt;
    }

}
