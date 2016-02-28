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

    private final Map<Enum<?>, String> entries = new HashMap<Enum<?>, String>();
    private final Map<String, String> keys2names = new HashMap<String, String>();

    public enum Names {
        Slave,
        Miss,
        Mistress,
        Sir,
        Master,
    }

    public final static TextVariables Defaults = createDefaults();

    private static TextVariables createDefaults() {
        TextVariables defaults = new TextVariables();
        defaults.put(Names.Slave, "slave");
        defaults.put(Names.Miss, "Miss");
        defaults.put(Names.Mistress, "Mistress");
        defaults.put(Names.Sir, "Sir");
        defaults.put(Names.Master, "Master");
        return defaults;
    }

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
        String textVariable = "(?i)#\\w+";
        Pattern p = Pattern.compile(textVariable,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        for (String text : texts) {
            Matcher m = p.matcher(text);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String key = m.group().substring(1);
                String replacement = get(key.toLowerCase());
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            expanded.add(sb.toString());
        }
        return expanded;
    }
}
