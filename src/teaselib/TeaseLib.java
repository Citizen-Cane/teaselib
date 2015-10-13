package teaselib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.core.Host;
import teaselib.core.Persistence;
import teaselib.core.Persistence.TextVariable;
import teaselib.core.ScriptInterruptedException;
import teaselib.util.Item;
import teaselib.util.Items;

/**
 * @author someone
 *
 *         Teaselib can either be used via the shared global instance, or by
 *         creating a private instance.
 * 
 *         A private TeaseLib instance might be useful when using more than one
 *         TTS voice in a scenario with multiple actors
 * 
 *         All static methods refer to the shared instance
 */
public class TeaseLib {
    public final Host host;
    private final Persistence persistence;

    private static TeaseLib instance;
    private Map<Class<?>, State<? extends Enum<?>>> states = new HashMap<Class<?>, State<? extends Enum<?>>>();

    private final boolean logDetails;
    private static BufferedWriter log = null;
    private final static File logFile = new File("./TeaseLib.log");
    private final static SimpleDateFormat timeFormat = new SimpleDateFormat(
            "HH:mm:ss.SSS");

    /**
     * Call this function from the host in order to initialize TeaseLib
     * 
     * @param host
     * @param persistence
     * @return The teaselib instance created during the initialization
     */
    public static TeaseLib init(Host host, Persistence persistence) {
        synchronized (TeaseLib.class) {
            if (instance == null) {
                instance = new TeaseLib(host, persistence);
            }
        }
        return instance;
    }

    public static TeaseLib instance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "Please create a TeaseLib instance first");
        }
        return instance;
    }

    private TeaseLib(Host host, Persistence persistence) {
        if (host == null || persistence == null) {
            throw new IllegalArgumentException();
        }
        this.host = host;
        this.persistence = persistence;
        // Init log
        try {
            log = new BufferedWriter(new FileWriter(logFile));
        } catch (IOException e) {
            host.show(null, "Cannot open log file " + logFile.getAbsolutePath());
            host.reply(Arrays.asList("Oh dear"));
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (log != null) {
                    try {
                        log.flush();
                        log.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        });
        logDetails = getBoolean(Config.Namespace, Config.Debug.LogDetails);
    }

    public static void log(String text) {
        synchronized (log) {
            Date now = new Date(System.currentTimeMillis());
            String line = timeFormat.format(now) + " -"
                    + Thread.currentThread().getName() + " - : " + text + "\n";
            try {
                if (log != null) {
                    log.write(line);
                    log.flush();
                }
            } catch (IOException e) {
                instance.host
                        .show(null,
                                "Cannot write to log file "
                                        + logFile.getAbsolutePath());
            }
            System.out.print(line);
        }
    }

    /**
     * Log details that might be interesting for developers
     * 
     * @param line
     *            Log output
     */
    public static void logDetail(String line) {
        if (instance().logDetails) {
            log(line);
        }
    }

    public static void logDetail(Object instance, Throwable e) {
        if (instance().logDetails) {
            log(instance, e);
        }
    }

    public static void log(Object instance, Throwable e) {
        String message = e.getMessage();
        log(e.getClass().getSimpleName() + ": "
                + (message != null ? message : ""));
        log(instance.getClass().getName() + ":" + instance.toString());
        logStackTrace(e);
        Throwable cause = e.getCause();
        if (cause != null) {
            log("Caused by");
            logStackTrace(cause);
        }
    }

    private static void logStackTrace(Throwable e) {
        for (StackTraceElement ste : e.getStackTrace()) {
            log("\t" + ste.toString());
        }
    }

    /**
     * Preferred method to wait, since it allows us to overwrite this method
     * with automated input.
     * 
     * If interrupted, must throw a ScriptInterruptedException. It's a runtime
     * exception so it doesn't have to be declared. This way simple scripts are
     * safe, but script closures can be cancelled.
     * 
     * @param milliseconds
     *            The time to sleep.
     */
    public void sleep(long x, TimeUnit timeUnit) {
        try {
            if (x > 0) {
                timeUnit.sleep(x);
            }
        } catch (InterruptedException e) {
            throw new ScriptInterruptedException();
        }
    }

    /**
     * @return Seconds since midnight 1.1.1970 UTC
     */
    public long getTime() {
        // todo change to millis
        return System.currentTimeMillis() / 1000;
    }

    public long getTime(TimeUnit unit) {
        return unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
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
    public int random(int min, int max) {
        double r = Math.random();
        return min + (int) (r * (max - min + 1));
    }

    /**
     * @author someone
     * 
     *         Handles elapsed seconds since object creation
     */
    public class Duration {
        public final long start;

        public Duration() {
            this.start = getTime(TimeUnit.SECONDS);
        }

        public Duration(long startSeconds) {
            this.start = startSeconds;
        }

        public long elapsed(TimeUnit unit) {
            long now = getTime(TimeUnit.SECONDS);
            long elapsedSeconds = now - start;
            return unit.convert(elapsedSeconds, TimeUnit.SECONDS);
        }

        public long elapsedSeconds() {
            return elapsed(TimeUnit.SECONDS);
        }

        public long elapsedMinutes() {
            return elapsed(TimeUnit.MINUTES);
        }

        public long elapsedHours() {
            return elapsed(TimeUnit.HOURS);
        }

        public long elapsedDays() {
            return elapsed(TimeUnit.DAYS);
        }
    }

    protected class PersistentValue {
        public final String name;

        protected PersistentValue(String namespace, String name) {
            this.name = makePropertyName(namespace, name);
        }

        protected PersistentValue(String namespace, Enum<?> name) {
            this.name = makePropertyName(namespace, name);
        }

        public void clear() {
            persistence.clear(name);
        }

        public boolean available() {
            return persistence.has(name);
        }
    }

    /**
     * @author someone
     * 
     *         A persistent boolean value, start value is false
     */
    public class PersistentBoolean extends PersistentValue {
        public final static boolean DefaultValue = false;

        private boolean defaultValue = DefaultValue;

        public PersistentBoolean(String namespace, String name) {
            super(namespace, name);
        }

        public PersistentBoolean(String namespace, Enum<?> name) {
            super(namespace, name);
        }

        public PersistentBoolean defaultValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public boolean get() {
            if (persistence.has(name)) {
                return persistence.getBoolean(name);
            } else {
                return defaultValue;
            }
        }

        @Override
        public void clear() {
            super.clear();
        }

        public void set() {
            set(true);
        }

        public void set(boolean value) {
            persistence.set(name, value);
        }
    }

    /**
     * @author someone
     * 
     *         A persistent integer value, start value is 0
     */
    public class PersistentInteger extends PersistentValue {
        public final static int DefaultValue = 0;

        private int defaultValue = DefaultValue;

        public PersistentInteger(String namespace, String name) {
            super(namespace, name);
        }

        public PersistentInteger(String namespace, Enum<?> name) {
            super(namespace, name);
        }

        public PersistentInteger defaultValue(int defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public int get() {
            final String value = persistence.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }

        public void set(int value) {
            persistence.set(name, Integer.toString(value));
        }
    }

    /**
     * @author someone
     * 
     *         A persistent float value, start value is 0.0
     */
    public class PersistentFloat extends PersistentValue {
        public final static double DefaultValue = 0.0;

        private double defaultValue = DefaultValue;

        public PersistentFloat(String namespace, String name) {
            super(namespace, name);
        }

        public PersistentFloat(String namespace, Enum<?> name) {
            super(namespace, name);
        }

        public PersistentFloat defaultValue(double defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public double get() {
            final String value = persistence.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }

        public void set(double value) {
            persistence.set(name, Double.toString(value));
        }
    }

    /**
     * @author someone
     * 
     *         A persistent String value, start value is the empty string
     */
    public class PersistentString extends PersistentValue {
        public final static String DefaultValue = "";

        private String defaultValue = DefaultValue;

        public PersistentString(String namespace, String name) {
            super(namespace, name);
        }

        public PersistentString(String namespace, Enum<?> name) {
            super(namespace, name);
        }

        public PersistentString defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public String get() {
            final String value = persistence.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                return value;
            }
        }

        public void set(String value) {
            persistence.set(name, value);
        }
    }

    public void clear(String namespace, String name) {
        persistence.clear(makePropertyName(namespace, name));
    }

    public void clear(String namespace, Enum<?> name) {
        persistence.clear(makePropertyName(namespace, name));
    }

    public void set(String namespace, Enum<?> name, boolean value) {
        persistence.set(makePropertyName(namespace, name), value);
    }

    public void set(String namespace, Enum<?> name, int value) {
        new PersistentInteger(namespace, name).set(value);
    }

    public void set(String namespace, Enum<?> name, double value) {
        new PersistentFloat(namespace, name).set(value);
    }

    public void set(String namespace, Enum<?> name, String value) {
        persistence.set(makePropertyName(namespace, name), value);
    }

    public void set(String namespace, String name, boolean value) {
        persistence.set(makePropertyName(namespace, name), value);
    }

    public void set(String namespace, String name, int value) {
        new PersistentInteger(namespace, name).set(value);
    }

    public void set(String namespace, String name, double value) {
        new PersistentFloat(namespace, name).set(value);
    }

    public void set(String namespace, String name, String value) {
        persistence.set(makePropertyName(namespace, name), value);
    }

    public boolean getBoolean(String namespace, String name) {
        return persistence.getBoolean(makePropertyName(namespace, name));
    }

    public double getFloat(String namespace, String name) {
        return new PersistentFloat(namespace, name).get();
    }

    public int getInteger(String namespace, String name) {
        return new PersistentInteger(namespace, name).get();
    }

    public String getString(String namespace, String name) {
        return persistence.get(makePropertyName(namespace, name));
    }

    public boolean getBoolean(String namespace, Enum<?> name) {
        return persistence.getBoolean(makePropertyName(namespace, name));
    }

    public double getFloat(String namespace, Enum<?> name) {
        return new PersistentFloat(namespace, name).get();
    }

    public int getInteger(String namespace, Enum<?> name) {
        return new PersistentInteger(namespace, name).get();
    }

    public String getString(String namespace, Enum<?> name) {
        return persistence.get(makePropertyName(namespace, name));
    }

    private static String makePropertyName(String namespace, String name) {
        return namespace + "." + name;
    }

    private static String makePropertyName(String namespace, Enum<?> name) {
        return namespace + "." + name.getClass().getSimpleName() + "."
                + name.name();
    }

    public <T extends Enum<?>> Item<T> getToy(T toy) {
        return item("toys", toy);
    }

    public <T extends Enum<?>> Item<T> getClothing(T item) {
        return item("clothes", item);
    }

    public <T> Item<T> getToy(T toy) {
        return item("toys", toy);
    }

    public <T> Item<T> getClothing(T item) {
        return item("clothes", item);
    }

    public String get(TextVariable name, String locale) {
        String value = persistence.get(name, locale);
        if (value != null) {
            return value;
        } else if (name.fallback != null) {
            return get(name.fallback, locale);
        }
        return name.toString();
    }

    public void set(TextVariable name, String locale, String value) {
        persistence.set(name, locale, value);
    }

    public class PersistentSequence<T extends Enum<T>> {
        public final PersistentString valueName;
        public final T[] values;
        private T value;

        public PersistentSequence(String namespace, String name, T[] values) {
            this.valueName = new PersistentString(namespace, name);
            this.values = values;
            String persistedValue = valueName.get();
            this.value = values[0];
            if (persistedValue != null) {
                for (T v : values) {
                    if (persistedValue.equals(v.name())) {
                        value = v;
                        break;
                    }
                }
            }
        }

        public T advance() {
            if (!completed()) {
                for (int i = 0; i < values.length; i++) {
                    if (value == values[i]) {
                        set(values[i + 1]);
                        break;
                    }
                }
            }
            return value;
        }

        public boolean completed() {
            return value == values[values.length - 1];
        }

        public T reset() {
            set(values[0]);
            return value;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
            valueName.set(value.name());
        }
    }

    /**
     * Return the state of an enumeration member
     * 
     * @param item
     *            The enumeration member to return the state for
     * @return The item state.
     */
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> State<T>.Item state(T item) {
        Class<Enum<?>> enumClass = (Class<Enum<?>>) item.getClass();
        final State<T> state = state(enumClass);
        final State<T>.Item stateItem;
        if (state.has(item)) {
            stateItem = state.get(item);
        } else {
            // If there is no entry, we can assume that the item hasn't been
            // used yet, or that the apply durations doens't matter
            // -> mark the item as not applied, and taken off a long time ago
            // -> checks against elapsed() do always succeed in this case,
            // allowing for simpler coding on the application layer
            stateItem = state.add(item, 0, 0);
        }
        return stateItem;
    }

    /**
     * Return the state for all or a subset of members of an enumeration.
     * 
     * @param values
     *            The values to retrieve the state for. This should be
     *            {@code Enum.values()}, as the state will only contain the
     *            state items for the listed values.
     * @return The state of all members of the {@code values} parameter
     */
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> State<T> state(T[] values) {
        Class<Enum<?>> enumClass = (Class<Enum<?>>) values[0].getClass();
        return state(enumClass);
    }

    /**
     * Return the state for all members of an enumeration.
     * 
     * @param enumClass
     *            The class of the enumeration.
     * @return The state of all members of the enumeration.
     */
    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> State<T> state(Class<Enum<?>> enumClass) {
        final State<T> state;
        if (states.containsKey(enumClass)) {
            state = (State<T>) states.get(enumClass);
        } else {
            state = new State<T>(this, enumClass);
            states.put(enumClass, state);
        }
        return state;
    }

    /**
     * Get items from a enumeration. This is different from toys and clothing in
     * that toys and clothing is usually handled by the host, whereas
     * script-related enumerations are handled by the script.
     * 
     * @param namespace
     *            The global namespace for the items, either the script name
     *            space or class name of the enumeration.
     * @param values
     * @return A list of items whose names are based on the enumeration members
     */
    public <T extends Enum<?>> Items<T> items(String namespace, T... values) {
        Items<T> items = new Items<T>(values.length);
        for (T v : values) {
            items.add(item(namespace, v));
        }
        return items;
    }

    /**
     * Get the item from an enumeration member
     * 
     * @param namespace
     *            The namespace of the item
     * @param value
     *            The enumeration value to get the item for
     * @return The item that corresponds to the enumeration member
     */
    public <T extends Enum<?>> Item<T> item(String namespace, T value) {
        return new Item<T>(value, new PersistentBoolean(namespace, value),
                Item.createDisplayName(value));
    }

    /**
     * Get the item for any object.
     * 
     * @param namespace
     *            The namespace of the item.
     * @param value
     *            The value to get the item for.
     * @return The item that corresponds to the value.
     */
    public <T> Item<T> item(String namespace, T value) {
        return new Item<T>(value, new PersistentBoolean(namespace,
                value.toString()), Item.createDisplayName(value));
    }
}
