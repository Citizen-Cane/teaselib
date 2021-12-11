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

import teaselib.Sexuality.Gender;
import teaselib.core.TeaseLib;

/**
 * 
 * Text variables are place holders in shown or spoken text, that are replaced at runtime. variables are embedded in
 * text by pre-fixing the variable name with a #, for instance "There you are, #slave".
 * <p>
 *
 */
public class TextVariables implements Iterable<String> {
    /**
     * How to address the submissive.
     *
     */
    public enum Identity {
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
        Slave_FullName

        ;

        public enum Alias {
            /**
             * Short-hand for name.
             */
            Slave
        }
    }

    /**
     * How to address the actor, to be used as manner of address in spoken language.
     *
     */
    public enum FormOfAddress {
        /**
         * The actor's title or honorific, as Mistress/Master, Miss/Sir
         */
        Title,

        /**
         * The actor's given name or title, but not both.
         */
        Name,
        /**
         * The actor's full or family name, prefixed by the title or honorific.
         */
        FullName

        ;

        enum Alias {
            Miss,
            Mistress,
            Sir,
            Master,
        }
    }

    private static final Pattern textVariablePattern = Pattern.compile("(?i)#\\w+",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final Map<String, String> keys2names = new LinkedHashMap<>();

    private final Map<String, String> aliases = new LinkedHashMap<>();

    public TextVariables(TextVariables... textVariables) {
        for (TextVariables tV : textVariables) {
            setAll(tV);
        }

        setAlias(Identity.Alias.Slave, Identity.Slave_Name);
        setAlias(FormOfAddress.Alias.Miss, FormOfAddress.Title);
        setAlias(FormOfAddress.Alias.Sir, FormOfAddress.Title);
        setAlias(FormOfAddress.Alias.Mistress, FormOfAddress.Title);
        setAlias(FormOfAddress.Alias.Master, FormOfAddress.Title);
    }

    public void setAll(TextVariables textVariables) {
        for (String var : textVariables) {
            keys2names.put(var, textVariables.get(var));
        }
        aliases.putAll(textVariables.aliases);
    }

    public String set(Enum<?> name, String value) {
        return keys2names.put(resolveAlias(name.name().toLowerCase()), value);
    }

    public String set(String name, String value) {
        return keys2names.put(resolveAlias(name.toLowerCase()), value);
    }

    public String setAlias(Enum<?> alias, Enum<?> name) {
        return setAlias(alias.name(), name.name());
    }

    public String setAlias(String alias, String name) {
        return aliases.put(alias.toLowerCase(), name.toLowerCase());
    }

    public void remove(Enum<?> name) {
        remove(resolveAlias(name.name()));
    }

    public void remove(String name) {
        keys2names.remove(resolveAlias(name.toLowerCase()));
    }

    @Override
    public Iterator<String> iterator() {
        return keys2names.keySet().iterator();
    }

    public boolean contains(String name) {
        return keys2names.containsKey(resolveAlias(name));
    }

    public String get(Enum<?> name) {
        return get(name.name());
    }

    public String get(String name) {
        return keys2names.get(resolveAlias(name).toLowerCase());
    }

    private String resolveAlias(String key) {
        return aliases.getOrDefault(key, key);
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

    public void setUserIdentity(TeaseLib teaseLib, String domain, Locale locale) {
        Gender gender = teaseLib.new PersistentEnum<>(domain, Gender.class).value();
        String language = locale.getLanguage();
        String namespace = userNamespace(gender);

        var name = formOfAddress(teaseLib, domain, namespace, FormOfAddress.Name, language);
        set(Identity.Slave_Name.name(), name);
        var title = formOfAddress(teaseLib, domain, namespace, FormOfAddress.Title, language);
        set(Identity.Slave_Title.name(), title);
        var fullname = formOfAddress(teaseLib, domain, namespace, FormOfAddress.FullName, language);
        set(Identity.Slave_FullName.name(), fullname);
    }

    private static String formOfAddress(TeaseLib teaseLib, String domain, String namespace, FormOfAddress formOfAddress,
            String language) {
        return teaseLib.getString(domain, namespace, qualifiedName(formOfAddress, language));
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
        return keys2names.toString();
    }

}
