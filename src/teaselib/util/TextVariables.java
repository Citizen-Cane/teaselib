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

public class TextVariables implements Iterable<Enum<?>> {
    /**
     * How to address the submissive.
     *
     */
    public enum Names {
        /**
         * Short-hand for slave title.
         */
        Slave,

        /**
         * slave title.
         */
        Slave_Title,

        /**
         * slave name.
         */
        Slave_Name,

        /**
         * slave full name.
         */
        Slave_FullName,

        User,
        User_Title,
        User_Name,
        USer_FullName
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

        String string = teaseLib.getString(domain, namespace, qualifiedName(Actor.FormOfAddress.Title, language));
        put(Names.Slave, string);
        put(Names.Slave_Title, string);
        put(Names.Slave_Name, teaseLib.getString(domain, namespace, qualifiedName(Actor.FormOfAddress.Name, language)));
        put(Names.Slave_FullName,
                teaseLib.getString(domain, namespace, qualifiedName(Actor.FormOfAddress.FullName, language)));
    }

    private static String userNamespace(Gender gender) {
        return qualifiedName(Names.User.name(), gender.name());
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
