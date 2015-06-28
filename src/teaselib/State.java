/**
 * 
 */
package teaselib;

import java.util.HashMap;
import java.util.Map;

import teaselib.TeaseLib.Duration;

// todo global get without script

/**
 * @author someone
 *
 */
public class State<T extends Enum<T>> {

    Map<T, Item> state = new HashMap<T, Item>();

    public State(TeaseLib teaseLib, T[] values) {
        super();
        for (T value : values) {
            String args = teaseLib.getString(value.name());
            if (args != null) {
                String[] argv = args.split(" ");
                add(value, Long.parseLong(argv[0]), Long.parseLong(argv[1]));
            }
        }
    }

    public class Item {
        final Object item;
        final Duration duration;
        final long howLong;

        public Item(T item) {
            this(item, null, 0);
        }

        public Item(T item, TeaseLib teaseLib, long howLong) {
            super();
            this.item = item;
            if (teaseLib != null) {
                this.duration = teaseLib.new Duration();
                this.howLong = howLong;
                teaseLib.set(item.getClass().getName() + "." + item.name(),
                        persisted(duration.startSeconds, howLong));
            } else {
                this.duration = null;
                this.howLong = 0;
            }
        }

        public Item(T item, long startSeconds, long howLong) {
            this.item = item;
            this.duration = TeaseLib.instance().new Duration(startSeconds);
            this.howLong = howLong;
        }
    }

    private static String persisted(long when, long howLong) {
        return when + " " + howLong;
    }

    public boolean has(T item) {
        return state.containsKey(item);
    }

    public void add(T item) {
        state.put(item, new Item(item));
    }

    public void add(T item, TeaseLib teaseLib, long howLong) {
        state.put(item, new Item(item, teaseLib, howLong));
    }

    private void add(T item, long startSeconds, long howLong) {
        state.put(item, new Item(item, startSeconds, howLong));
    }

    public void remove(T item) {
        state.remove(item);
    }

    public Item get(T item) {
        return state.get(item);
    }

    public Map<T, Item> expired() {
        Map<T, Item> items = new HashMap<T, Item>();
        for (Map.Entry<T, Item> entry : state.entrySet()) {
            State<T>.Item item = entry.getValue();
            Duration duration = item.duration;
            if (duration != null) {
                if (duration.elapsedSeconds() >= item.howLong) {
                    items.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return items;
    }

    public Map<T, Item> remaining() {
        Map<T, Item> items = new HashMap<T, Item>();
        for (Map.Entry<T, Item> entry : state.entrySet()) {
            State<T>.Item item = entry.getValue();
            Duration duration = item.duration;
            if (duration != null) {
                if (duration.elapsedSeconds() < item.howLong) {
                    items.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return items;
    }
}
