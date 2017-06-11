package teaselib.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CommandLineParameters<T extends Enum<?>> extends HashMap<T, List<String>> {
    private static final long serialVersionUID = 1L;

    public static final List<String> EMPTY = Collections.EMPTY_LIST;

    protected T defaultKeyword;

    public CommandLineParameters(String[] args, T[] keywords) {
        this(Arrays.asList(args), keywords);
    }

    public CommandLineParameters(List<String> args, T[] keywords) {
        this.defaultKeyword = keywords[0];
        put(defaultKeyword, new ArrayList<String>());
        T currentKeyword = defaultKeyword;
        for (String arg : args) {
            T keyword = getKeyword(arg, keywords);
            if (keyword != null) {
                currentKeyword = keyword;
                put(currentKeyword, new ArrayList<String>());
            } else {
                get(currentKeyword).add(arg);
            }
        }
    }

    @Override
    public boolean containsKey(Object value) {
        return get(value) != EMPTY;
    }

    @Override
    public List<String> get(Object value) {
        return super.getOrDefault(value, EMPTY);
    }

    public static <E extends Enum<?>> E getKeyword(String arg, E[] keywords) {
        for (E keyword : keywords) {
            if (keyword.name().equalsIgnoreCase(arg)) {
                return keyword;
            }
        }
        return null;
    }

    public List<String> getItems() {
        return get(defaultKeyword);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = superHashCodeCaseIndependent();
        result = prime * result + ((defaultKeyword == null) ? 0 : defaultKeyword.hashCode());
        return result;
    }

    private int superHashCodeCaseIndependent() {
        final int prime = 31;
        int result = 1;
        for (java.util.Map.Entry<T, List<String>> entry : this.entrySet()) {
            result = prime * result * entry.getKey().hashCode();
            for (String string : entry.getValue()) {
                result = prime * result * string.toLowerCase().hashCode();
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        CommandLineParameters<?> other = (CommandLineParameters<?>) obj;
        if (!superEqualsIgnoreCase(other))
            return false;
        if (defaultKeyword == null) {
            if (other.defaultKeyword != null)
                return false;
        } else if (!defaultKeyword.equals(other.defaultKeyword))
            return false;
        return true;
    }

    private boolean superEqualsIgnoreCase(CommandLineParameters<?> other) {
        for (java.util.Map.Entry<T, List<String>> entry : this.entrySet()) {
            if (!other.containsKey(entry.getKey())) {
                return false;
            } else {
                if (!listsEqualsCaseIndependent(entry.getValue(), other.get(entry.getKey())))
                    return false;
            }
        }

        for (java.util.Map.Entry<?, List<String>> entry : other.entrySet()) {
            if (!other.containsKey(entry.getKey())) {
                return false;
            }
        }

        return true;
    }

    private static boolean listsEqualsCaseIndependent(List<String> mine, List<String> other) {
        if (mine.size() != other.size()) {
            return false;
        }

        for (String string : mine) {
            boolean stringFound = false;
            for (String otherString : other) {
                if (string.equalsIgnoreCase(otherString)) {
                    stringFound = true;
                    break;
                }
            }
            if ((!stringFound)) {
                return false;
            }
        }
        return true;
    }

}
