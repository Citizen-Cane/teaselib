package teaselib.util.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import teaselib.util.Interval;
import teaselib.util.Item;
import teaselib.util.Items;

public class Random {

    private final RandomGenerator generator;

    public Random() {
        this(new java.util.Random(System.currentTimeMillis()));

    }

    public Random(RandomGenerator generator) {
        this.generator = generator;
    }

    public void chance(Runnable script) {
        if (chance()) {
            script.run();
        }
    }

    public boolean chance() {
        return chance(0.5f);
    }

    public boolean chance(float probability) {
        double r = generator.nextDouble();
        return probability > r;
    }

    /**
     * Return a random number
     * 
     * @param min
     *            minimum value
     * @param max
     *            maximum value
     * @return A value in the interval [min, max]
     */
    public int value(int min, int max) {
        return generator.nextInt(max - min + 1) + min;
    }

    public int value(Interval interval) {
        return value(interval.start, interval.end);
    }

    public double value(double min, double max) {
        double r = generator.nextDouble();
        return scale(r, min, max);
    }

    public double scale(double value, double min, double max) {
        return min + (value * (max - min));
    }

    @SafeVarargs
    public final <T> T item(T... items) {
        return item(Arrays.asList(items));
    }

    @SafeVarargs
    public final <T> T another(T current, T... items) {
        return item(current, Arrays.asList(items));
    }

    public <T> T item(List<T> items) {
        if (items == null)
            throw new NullPointerException();
        else if (items.isEmpty())
            throw new IllegalArgumentException("Empty list");

        return items.get(value(0, items.size() - 1));
    }

    public <T> T item(T current, List<T> items) {
        Objects.requireNonNull(items);
        if (items.isEmpty())
            return current;

        T another;
        int index = value(0, items.size() - 1);
        another = items.get(index);
        if (another == current) {
            if (index == items.size() - 1) {
                another = items.get(0);
            } else {
                another = items.get(index + 1);
            }
        }
        return another;
    }

    /**
     * Selects n random elements while preserving the order of those elements. If elements.size() is smaller than n, all
     * elements are returned.
     * 
     * @param <T>
     * @param elements
     *            Elements to choose from
     * @param n
     *            Number of elements to choose
     * @return The selected n elements. If elements.size() is smaller than n, all elements are returned.
     */
    public <T> List<T> orderedElements(List<T> elements, int n) {
        List<T> selected = new ArrayList<>(elements);
        int k = elements.size() - n;
        if (k <= 0)
            return selected;
        for (int i = 0; i < k; i++) {
            int index = value(0, selected.size() - 1);
            selected.remove(index);
        }
        return selected;
    }

    public <T> List<T> randomized(T[] items, int elements) {
        return items(null, 0, items, elements);
    }

    /**
     * Build a list with n randomized entries, starting with all the shuffled items in introduction, followed by random
     * items of repetitions.
     * 
     * @param introduction
     * @param repetition
     * @param size
     *            Requested size of the resulting list, at least introduction.size()
     * @return The list starting with all the shuffled items from the introduction, followed by items from repetition
     *         until the requested count is reached. Repetitions are never added one after the other.
     */
    public <T> List<T> items(T[] introduction, T[] repetition, int size) {
        return items(introduction, introduction.length, repetition, Math.max(0, size - introduction.length));
    }

    /**
     * Build a list with n randomized entries, starting with all the shuffled items in introduction, followed by random
     * items of repetitions.
     * 
     * @param introduction
     * @param introductionElements
     *            Number of randomized elements from the introduction
     * @param repetition
     * @param repetitionElements
     *            Number of randomized elements from the repetition.
     * @return The list starting with some shuffled items from the introduction until the requested count is reached,
     *         followed by some items from repetition until the requested count is reached. Repetitions are never added
     *         one after the other.
     */
    public <T> List<T> items(T[] introduction, int introductionElements, T[] repetition, int repetitionElements) {
        List<T> out = new ArrayList<>(repetitionElements);
        if (introduction != null) {
            if (introduction.length > 0) {
                addRandomizedElements(out, introduction, introductionElements);
            }
        }
        if (repetition != null) {
            if (repetition.length > 0) {
                addRandomizedElements(out, repetition, repetitionElements);
            }
        }
        return out;
    }

    private <T> List<T> addRandomizedElements(List<T> list, T[] elements, int n) {
        T last = !list.isEmpty() ? list.get(list.size() - 1) : null;
        for (int i = 0; i < n; i++) {
            T t = null;
            while (last == (t = elements[value(0, elements.length - 1)]) && elements.length > 1) {
                // Don't add the same entry twice in a row
            }
            list.add(t);
            last = t;
        }
        return list;
    }

    public Item item(Items items) {
        List<Item> itemList = items.stream().collect(Collectors.toList());
        if (!itemList.isEmpty()) {
            return itemList.get(value(0, itemList.size() - 1));
        } else {
            return Item.NotFound;
        }
    }

}
