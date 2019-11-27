package teaselib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.TeaseLib;
import teaselib.core.TimeOfDay;
import teaselib.util.Daytime;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * @author Citizen-Cane
 *
 */
public class TeaseScriptMath extends TeaseScriptPersistenceUtil {

    protected TeaseScriptMath(TeaseLib teaseLib, ResourceLoader resources, Actor actor, String namespace) {
        super(teaseLib, resources, actor, namespace);
    }

    protected TeaseScriptMath(Script script, Actor actor) {
        super(script, actor);
    }

    public Message message(String... text) {
        if (text == null)
            return null;
        if (text.length == 0)
            return new Message(actor);
        return new Message(actor, text);
    }

    public Message message(List<String> text) {
        if (text == null)
            return null;
        return new Message(actor, text);
    }

    /**
     * Return a random number
     * 
     * @param min
     * @param max
     * @return A value in the interval [min, max]
     */
    public int random(int min, int max) {
        return teaseLib.random(min, max);
    }

    public double random(double min, double max) {
        return teaseLib.random(min, max);
    }

    @SafeVarargs
    public final <T> T random(T... items) {
        if (items == null)
            return null;
        if (items.length == 0)
            return null;
        return items[random(0, items.length - 1)];
    }

    public <T> T random(List<T> items) {
        if (items == null)
            throw new NullPointerException();
        else if (items.isEmpty())
            throw new IllegalArgumentException("Empty list");

        return items.get(random(0, items.size() - 1));
    }

    public <T> T random(Collection<T> items) {
        if (items == null)
            throw new NullPointerException();
        else if (items.isEmpty())
            throw new IllegalArgumentException("Empty list");

        int s = random(0, items.size() - 1);
        Iterator<T> iterator = items.iterator();
        T item = null;
        for (int i = 0; i < s; i++) {
            iterator.next();
        }
        return item;
    }

    public <T> List<T> randomized(T[] items, int elements) {
        return randomized(null, 0, items, elements);
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
    public <T> List<T> randomized(T[] introduction, T[] repetition, int size) {
        return randomized(introduction, introduction.length, repetition, Math.max(0, size - introduction.length));
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
    public <T> List<T> randomized(T[] introduction, int introductionElements, T[] repetition, int repetitionElements) {
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
            while (last == (t = elements[random(0, elements.length - 1)])) {
                // Don't add the same entry twice in a row
            }
            list.add(t);
            last = t;
        }
        return list;
    }

    public void sleep(long duration, TimeUnit timeUnit) {
        teaseLib.sleep(duration, timeUnit);
    }

    public Duration duration() {
        return teaseLib.duration();
    }

    public Duration duration(long limit, TimeUnit unit) {
        return teaseLib.duration(limit, unit);
    }

    public Duration duration(Daytime dayTime) {
        return teaseLib.duration(dayTime);
    }

    public Duration duration(Daytime dayTime, long daysInTheFuture) {
        return teaseLib.duration(dayTime, daysInTheFuture);
    }

    public Item random(Items items) {
        List<Item> itemList = items.stream().collect(Collectors.toList());
        if (!itemList.isEmpty()) {
            return itemList.get(random(0, itemList.size() - 1));
        } else {
            return Item.NotFound;
        }
    }

    public TimeOfDay timeOfDay() {
        return teaseLib.timeOfDay();
    }
}
