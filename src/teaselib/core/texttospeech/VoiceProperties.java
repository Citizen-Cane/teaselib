package teaselib.core.texttospeech;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import teaselib.TeaseLib;
import teaselib.core.util.SortedProperties;

public class VoiceProperties {
    protected final SortedProperties properties = new SortedProperties();

    void clear() {
        properties.clear();
    }

    boolean empty() {
        return properties.size() == 0;
    }

    public void putGuid(String key, Voice voice) {
        properties.put(key + ".guid", voice.guid);
    }

    public void put(String key, Voice voice) {
        properties.put(key + ".guid", voice.guid);
        properties.put(key + ".name", voice.name);
        properties.put(key + ".locale", voice.locale);
        properties.put(key + ".language", voice.language);
        properties.put(key + ".gender", voice.gender.toString());
    }

    public String getGuid(String key) {
        return (String) properties.get(key + ".guid");
    }

    public String getName(String key) {
        return (String) properties.get(key + ".name");
    }

    public String getLangID(String key) {
        return (String) properties.get(key + ".locale");
    }

    public String getLanguage(String key) {
        return (String) properties.get(key + ".language");
    }

    public Set<String> keySet() {
        Set<String> keys = new HashSet<String>();
        for (Object k : properties.keySet()) {
            String key = (String) k;
            int pos = key.lastIndexOf(".");
            {
                if (pos < 0) {
                    keys.add(key);
                } else {
                    keys.add(key.substring(0, pos));
                }
            }
        }
        return keys;
    }

    protected void store(File path, String fileName) throws IOException {
        File file = new File(path, fileName);
        TeaseLib.instance().log.info("Saving " + file.toString());
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            properties.store(fileOutputStream, "");
        } finally {
            if (fileOutputStream != null)
                fileOutputStream.close();
        }
    }
}
