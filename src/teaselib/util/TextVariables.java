package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextVariables implements Iterable<Enum<?>> {
    public enum Names {
        /**
         * Name of the slave.
         */
        Slave,
    }

    public final static TextVariables Defaults = createDefaults();

    private static TextVariables createDefaults() {
        TextVariables defaults = new TextVariables();
        defaults.put(Names.Slave, "slave");
        return defaults;
    }

    private final static Pattern textVariablePattern = Pattern.compile(
            "(?i)#\\w+", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final Map<Enum<?>, String> entries = new HashMap<Enum<?>, String>();
    private final Map<String, String> keys2names = new HashMap<String, String>();

    public TextVariables(TextVariables... textVariables) {
        for (TextVariables tV : textVariables) {
            addAll(tV);
        }
    }

    public void addAll(TextVariables textVariables) {
        entries.putAll(textVariables.entries);
        for (Enum<?> var : textVariables) {
            keys2names.put(var.name().toLowerCase(), entries.get(var));
        }
    }

    public String put(Enum<?> key, String value) {
        entries.put(key, value);
        return keys2names.put(key.name().toLowerCase(), value);
    }

    @Override
    public Iterator<Enum<?>> iterator() {
        return entries.keySet().iterator();
    }

    public boolean contains(Enum<?> key) {
        return entries.containsKey(key);
    }

    public boolean contains(String key) {
        return keys2names.containsKey(key);
    }

    public String get(Enum<?> key) {
        return entries.get(key);
    }

    public String get(String key) {
        return keys2names.get(key);
    }

    public String expand(String text) {
        return expand(Arrays.asList(text)).get(0);
    }

    public List<String> expand(List<String> texts) {
        List<String> expanded = new ArrayList<String>(texts.size());
        for (String text : texts) {
            Matcher m = textVariablePattern.matcher(text);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String name = m.group().substring(1);
                String key = name.toLowerCase();
                String replacement = contains(key) ? get(key) : name;
                boolean undefined = replacement == null;
                if (undefined) {
                    replacement = name;
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            expanded.add(sb.toString());
        }
        return expanded;
    }
}
