package teaselib.util.math;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;

import teaselib.util.Items;

/**
 * @author Citizen-Cane
 *
 */
public class Combinations<T> extends ArrayList<T> {
    private static final long serialVersionUID = 1L;

    private static <T> void combinations2(T[] arr, int len, int startPosition, T[] result, List<T[]> combinations) {
        if (len == 0) {
            combinations.add(Arrays.copyOf(result, result.length));
        } else {
            for (int i = startPosition; i <= arr.length - len; i++) {
                result[result.length - len] = arr[i];
                combinations2(arr, len - 1, i + 1, result, combinations);
            }
        }
    }

    @SafeVarargs
    public static <T> Combinations<T[]> combinations1toN(T... items) {
        Combinations<T[]> combinations = new Combinations<>();
        for (int k = 1; k <= items.length; k++) {
            combinations.addAll(combinationsK(k, items));
        }
        return combinations;
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> Combinations<T[]> combinationsK(int k, T... items) {
        Combinations<T[]> combinations = new Combinations<>();
        T[] result = (T[]) Array.newInstance(items[0].getClass(), k);
        combinations2(items, k, 0, result, combinations);
        return combinations;
    }

    public static Collector<Items, Combinations<Items>, Combinations<Items>> toCombinations() {
        return Collector.of(Combinations<Items>::new, //
                (items, item) -> items.add(item), //
                (items1, items2) -> {
                    items1.addAll(items2);
                    return items1;
                }, Collector.Characteristics.UNORDERED);
    }

    public T reduce(BinaryOperator<T> accumulator) {
        return stream().reduce(get(0), accumulator);
    }
}
