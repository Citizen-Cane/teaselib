package teaselib.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class QualifiedItems {

    private QualifiedItems() {
        super();
    }

    public static List<QualifiedItem> of(Object... values) {
        return Arrays.stream(values).map(QualifiedItem::of).collect(Collectors.toList());
    }

    public static List<QualifiedItem> of(Set<Object> values) {
        return values.stream().map(QualifiedItem::of).collect(Collectors.toList());
    }

    public static List<QualifiedItem> of(List<Object> values) {
        return values.stream().map(QualifiedItem::of).collect(Collectors.toList());
    }
}
