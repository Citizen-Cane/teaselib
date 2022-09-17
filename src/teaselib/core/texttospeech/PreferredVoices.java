/**
 * 
 */
package teaselib.core.texttospeech;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * @author Citizen-Cane
 *
 */
public class PreferredVoices {
    Properties voices;

    public PreferredVoices() {
        super();
        voices = new Properties();
    }

    public PreferredVoices(File path) throws FileNotFoundException, IOException {
        super();
        voices = new Properties();
        load(path);
    }

    public PreferredVoices load(File path) throws FileNotFoundException, IOException {
        try (FileInputStream fileInputStream = new FileInputStream(path);) {
            return load(fileInputStream);
        }
    }

    public PreferredVoices load(FileInputStream fileInputStream) throws IOException {
        voices.load(fileInputStream);
        return this;
    }

    enum Filter {
        Preferred,
        Ignored
    }

    public Map<String, String> getPreferredVoiceGuids() {
        List<Entry<String, String>> list = get(Filter.Preferred);
        return sortByValue(list);
    }

    public Set<String> getDisabledVoiceGuids() {
        List<Entry<String, String>> list = get(Filter.Ignored);
        return sortByValue(list).keySet();
    }

    private List<Map.Entry<String, String>> get(Filter filter) {
        List<Map.Entry<String, String>> list = new LinkedList<>();
        for (Entry<Object, Object> entry : voices.entrySet()) {
            if (filter.equals(Filter.Ignored)) {
                if (Integer.parseInt(entry.getValue().toString()) < 0) {
                    list.add(new AbstractMap.SimpleImmutableEntry<>(Objects.toString(entry.getKey()), Objects.toString(entry.getValue())));
                }
            } else if (filter.equals(Filter.Preferred)) {
                if (Integer.parseInt(entry.getValue().toString()) >= 0) {
                    list.add(new AbstractMap.SimpleImmutableEntry<>(Objects.toString(entry.getKey()), Objects.toString(entry.getValue())));
                }
            }
        }
        return list;
    }

    // http://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values-java
    // TODO port to java8 once upgraded
    static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(List<Map.Entry<K, V>> list) {
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                Integer value1 = Integer.valueOf(o1.getValue().toString());
                Integer value2 = Integer.valueOf(o2.getValue().toString());
                return value2.compareTo(value1);
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public void clear() {
        voices.clear();
    }

}
