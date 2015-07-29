/**
 * 
 */
package teaselib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import teaselib.text.Message;

/**
 * @author someone
 *
 */
public class TeaseScriptMath extends TeaseScriptPersistence {

    public TeaseScriptMath(TeaseLib teaseLib, Actor actor, String namespace) {
        super(teaseLib, actor, namespace);
    }

    public TeaseScriptMath(TeaseScriptBase script, Actor actor) {
        super(script, actor);
    }

    public <T> T[] items(T... items) {
        return items;
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

    public <T> T random(T... items) {
        if (items == null)
            return null;
        if (items.length == 0)
            return null;
        return items[random(0, items.length - 1)];
    }

    public <T> T random(List<T> items) {
        if (items == null)
            return null;
        if (items.size() == 0)
            return null;
        return items.get(random(0, items.size() - 1));
    }

    public <T> T random(Collection<T> items) {
        if (items == null)
            return null;
        if (items.size() == 0)
            return null;
        int s = random(0, items.size() - 1);
        Iterator<T> iterator = items.iterator();
        T item = null;
        for (int i = 0; i < s; i++) {
            iterator.next();
        }
        return item;
    }

    public <T> List<T> randomized(T[] items, int n) {
        @SuppressWarnings("unchecked")
        T[] introduction = (T[]) new Object[0];
        return randomized(introduction, items, n);
    }

    /**
     * Build a list with n randomized entries, starting with the shuffled items
     * in introduction, followed by random items of repetitions.
     * 
     * @param introduction
     * @param repeations
     * @param n
     *            Requested size of the resulting list, at least
     *            introduction.size()
     * @return The list starting with the shuffled items from the introduction,
     *         followed by items from repetition. Repetitions are never added
     *         twice in a row.
     */
    public <T> List<T> randomized(T[] introduction, T[] repetitions, int n) {
        List<T> out = new ArrayList<T>(n);
        if (introduction != null) {
            out.addAll(Arrays.asList(introduction));
            Collections.shuffle(out);
        }
        T last = out.size() > 0 ? out.get(out.size() - 1) : null;
        for (int i = out.size(); i < n; i++) {
            T t = null;
            // Don't add the same entry twice in a row
            while (last == (t = repetitions[random(0, repetitions.length - 1)])) {
            }
            out.add(t);
            last = t;
        }
        return out;
    }

    public void sleep(long duration, TimeUnit timeUnit) {
        teaseLib.sleep(duration, timeUnit);
    }

    public TeaseLib.Duration duration() {
        return teaseLib.new Duration();
    }

}
