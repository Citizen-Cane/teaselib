package teaselib.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CommandLineParameters<T extends Enum<?>> extends HashMap<T, List<String>> {
    private static final long serialVersionUID = 1L;

    public static final List<String> EMPTY = Collections.EMPTY_LIST;

    public CommandLineParameters(String[] args, T[] keywords) {
        T currentKeyword = null;
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

    public List<String> getLeading() {
        return get(null);
    }
}
