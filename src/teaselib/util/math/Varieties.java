package teaselib.util.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;

import teaselib.util.Items;

/**
 * @author Citizen-Cane
 *
 */
public class Varieties<T> extends ArrayList<T> {
    private static final long serialVersionUID = 1L;

    public T reduce(BinaryOperator<T> accumulator) {
        return stream().reduce(get(0), accumulator);
    }

    public static Collector<Items, Varieties<Items>, Varieties<Items>> toVarieties() {
        return Collector.of(Varieties<Items>::new, //
                (items, item) -> items.add(item), //
                (items1, items2) -> {
                    items1.addAll(items2);
                    return items1;
                }, Collector.Characteristics.UNORDERED);
    }

    public static <T> boolean isVariety(Collection<T> collection) {
        Set<T> set = new HashSet<>();
        for (T t : collection) {
            if (!set.contains(t)) {
                set.add(t);
            } else {
                return false;
            }
        }
        return true;
    }
}
