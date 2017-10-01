package teaselib.core.texttospeech;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;

public class PronounciationDictionary {
    final File rootDirectory;
    final Map<Voice, Map<String, String>> cache = new HashMap<>();

    public static PronounciationDictionary empty() {
        return new PronounciationDictionary(null);
    }

    public PronounciationDictionary(File root) {
        super();
        this.rootDirectory = root;
    }

    public Map<String, String> pronounciations(Voice voice) throws IOException {
        if (cache.containsKey(voice)) {
            return cache.get(voice);
        } else {
            Map<String, String> pronounciations = pronounciations(voice.api, voice.vendor, voice.locale, voice.guid);
            cache.put(voice, pronounciations);
            return pronounciations;
        }
    }

    Map<String, String> pronounciations(String api, String vendor, String locale, String voiceGuid) throws IOException {
        Map<String, String> all = new HashMap<>();

        List<File> dictionaries = dictionaries(api, vendor, locale, voiceGuid);

        for (File file : dictionaries) {
            if (file.exists()) {
                Properties p = new Properties();
                try (FileInputStream fis = new FileInputStream(file)) {
                    p.load(fis);
                    for (Entry<Object, Object> entry : p.entrySet()) {
                        String key = (String) entry.getKey();
                        all.put(key, (String) entry.getValue());
                    }
                }
            }
        }

        return all;
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
