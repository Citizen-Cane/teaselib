package teaselib.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class SortedProperties extends Properties {

    private static final long serialVersionUID = 1L;

    private final transient Comparator<String> sortOrder = String::compareToIgnoreCase;

    public SortedProperties() {
        super();
    }

    public SortedProperties(Properties defaults) {
        super(defaults);
    }

    @Override
    public Set<java.util.Map.Entry<Object, Object>> entrySet() {
        List<java.util.Map.Entry<Object, Object>> entries = new ArrayList<>(super.entrySet());
        Collections.sort(entries, (a, b) -> sortOrder.compare(a.getKey().toString(), b.getKey().toString()));
        return new LinkedHashSet<>(entries);
    }

}
