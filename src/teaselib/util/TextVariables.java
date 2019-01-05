package teaselib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.core.TeaseLib;

/**
 * 
 * Text variables are place holders in shown or spoken text, that are replaced at runtime. variables are embedded in
 * text by pre-fixing the variable name with a #, for instance "There you are, #slave".
 * <p>
 *
 */
public class TextVariables implements Iterable<Enum<?>> {
    /**
     * How to address the submissive.
     *
     */
    public enum Slave {
        /**
         * Short-hand for name.
         */
        Slave,

        /**
         * title.
         */
        Slave_Title,

        /**
         * given name, nick name or title, only one of them.
         */
        Slave_Name,

        /**
         * 
         * full name, for instance "#title #name"
         */
        Slave_FullName,
    }

    private static final Pattern textVariablePattern = Pattern.compile("(?i)#\\w+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final Map<Enum<?>, String> entries = new LinkedHashMap<>();
    private final Map<String, String> keys2names = new LinkedHashMap<>();

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

    public void remove(Enum<?> key) {
        entries.remove(key);
        keys2names.remove(key.name().toLowerCase());
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

    public List<String> expand(List<String> texts) {
        List<String> expanded = new ArrayList<>(texts.size());
        for (String text : texts) {
            expanded.add(expand(text));
        }
        return expanded;
    }

    public String expand(String text) {
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
        return sb.toString();
    }

    public void addUserIdentity(TeaseLib teaseLib, String domain, Locale locale) {
        Gender gender = teaseLib.new PersistentEnum<>(domain, Gender.class).value();
        String language = locale.getLanguage();
        String namespace = userNamespace(gender);

        String name = teaseLib.getString(domain, namespace, qualifiedName(Actor.FormOfAddress.Name, language));
        // TODO Slave.Slave is an alias, not a type -> must be handled as such,
        // since multiple entries for the same block name changes in script
        // (for an example on how scripts handle this see Rakhee Maid training)
        // TODO Simplify names: Slave -> Alias to Slave_Name
        // TODO remove "Slave_" from all other entries, but keep the text
        // variable name pattern "Slave_VAR"
        put(Slave.Slave, name);
        put(Slave.Slave_Name, name);
        put(Slave.Slave_Title,
                teaseLib.getString(domain, namespace, qualifiedName(Actor.FormOfAddress.Title, language)));
        put(Slave.Slave_FullName,
                teaseLib.getString(domain, namespace, qualifiedName(Actor.FormOfAddress.FullName, language)));
    }

    private static String userNamespace(Gender gender) {
        return qualifiedName("user", gender.name());
    }

    private static String qualifiedName(Enum<?> part1, String part2) {
        return qualifiedName(part1.name(), part2);
    }

    private static String qualifiedName(String... parts) {
        Iterator<String> part = Arrays.asList(parts).iterator();
        StringBuilder qualifiedName = new StringBuilder(part.next().toLowerCase());
        while (part.hasNext()) {
            qualifiedName.append(".");
            qualifiedName.append(part.next().toLowerCase());
        }
        return qualifiedName.toString();
    }

    @Override
    public String toString() {
        return entries.toString();
    }

}
