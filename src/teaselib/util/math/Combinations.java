package teaselib.util.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Citizen-Cane
 *
 */
public class Combinations {
    private static <T> void combinations2(T[] arr, int len, int startPosition, T[] result, List<T[]> combinations) {
        if (len == 0) {
            combinations.add(Arrays.copyOf(result, result.length));
            return;
        } else {
            for (int i = startPosition; i <= arr.length - len; i++) {
                result[result.length - len] = arr[i];
                combinations2(arr, len - 1, i + 1, result, combinations);
            }
        }
    }

    @SafeVarargs
    public static <T> List<T[]> combinations1toN(T... items) {
        List<T[]> combinations = new ArrayList<>();
        for (int k = 1; k <= items.length; k++) {
            combinations.addAll(combinationsK(k, items));
        }
        return combinations;
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> List<T[]> combinationsK(int k, T... items) {
        List<T[]> combinations = new ArrayList<>();
        combinations2(items, k, 0, (T[]) new Object[k], combinations);
        return combinations;
    }
}
