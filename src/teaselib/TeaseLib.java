package teaselib;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.persistence.Item;
import teaselib.speechrecognition.SpeechRecognizer;
import teaselib.texttospeech.TextToSpeechPlayer;

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

    private static TeaseLib instance;

    public final Host host;
    public final ResourceLoader resources;
    public final Persistence persistence;

    public final SpeechRecognizer speechRecognizer;
    public final TextToSpeechPlayer speechSynthesizer;

    public final static boolean logDetails = false;

    private static BufferedWriter log = null;
    private final static File logFile = new File("./TeaseLib.log");

    public TeaseLib(TeaseLib teaseLib, String assetRoot) {
        this(teaseLib.host, teaseLib.persistence, teaseLib.resources.basePath,
                assetRoot);
    }

    public TeaseLib(Host host, Persistence persistence, String basePath,
            String assetRoot) {
        if (host == null || persistence == null || basePath == null
                || assetRoot == null) {
            throw new IllegalArgumentException();
        }
        this.host = host;
        this.persistence = persistence;
        TeaseLib.instance = this;
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
        // Now we can log errors
        try {
            this.resources = new ResourceLoader(basePath, assetRoot);
        } catch (Throwable t) {
            log(this, t);
            throw new RuntimeException(t);
        }
        speechRecognizer = new SpeechRecognizer();
        speechSynthesizer = new TextToSpeechPlayer(this, speechRecognizer);

    }

    public static TeaseLib instance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "Please create a TeaseLib instance first");
        }
        return instance;
    }

    public static Host host() {
        return instance().host;
    }

    public static ResourceLoader resources() {
        return instance().resources;
    }

    public static Persistence persistence() {
        return instance().persistence;
    }

    public static synchronized void log(String text) {
        Date now = new Date(System.currentTimeMillis());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        String line = timeFormat.format(now) + ": " + text + "\n";
        try {
            if (log != null) {
                log.write(line);
            }
        } catch (IOException e) {
            host().show(null,
                    "Cannot write to log file " + logFile.getAbsolutePath());
        }
        System.out.println(line);
    }

    /**
     * Log details interesting for teaselib devs
     * 
     * @param line
     *            Log output
     */
    public static void logDetail(String line) {
        if (logDetails) {
            log(line);
        }
    }

    public static void logDetail(Object instance, Throwable e) {
        if (logDetails) {
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

    public void addAssets(String... assets) {
        resources.addAssets(assets);
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
            this.start = getTime();
        }

        public Duration(long startSeconds) {
            this.start = startSeconds;
        }

        public long elapsed(TimeUnit unit) {
            long now = getTime();
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
        protected final String name;

        public PersistentValue(String name) {
            this.name = name;
        }
    }

    /**
     * @author someone
     * 
     *         A persistent boolean value, start value is false
     */
    public class PersistentFlag extends PersistentValue {
        public PersistentFlag(String name) {
            super(name);
        }

        public boolean get() {
            return persistence.getBoolean(name);
        }

        public void clear() {
            set(false);
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
    public class PersistentNumber extends PersistentValue {
        public PersistentNumber(String name) {
            super(name);
        }

        public int get() {
            try {
                return Integer.parseInt(persistence.get(name));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public void set(int value) {
            persistence.set(name, Integer.toString(value));
        }
    }

    /**
     * @author someone
     * 
     *         A persistent String value, start value is the empty string
     */
    public class PersistentString extends PersistentValue {
        public PersistentString(String name) {
            super(name);
        }

        public String get() {
            return persistence.get(name);
        }

        public void set(String value) {
            persistence.set(name, value);
        }
    }

    public class PersistentSequence<T extends Enum<T>> {
        public final String name;
        public final T[] values;
        private T value;

        public PersistentSequence(String name, T[] values) {
            this.name = name;
            this.values = values;
            String persistedValue = getString(name);
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
            TeaseLib.this.set(name, value.toString());
        }
    }

    public boolean flag(String name) {
        return new PersistentFlag(name).get();
    }

    public void set(String name, boolean value) {
        new PersistentFlag(name).set(value);
    }

    public void set(String name, int value) {
        new PersistentNumber(name).set(value);
    }

    public void set(String name, String value) {
        new PersistentString(name).set(value);
    }

    public int getInteger(String name) {
        return new PersistentNumber(name).get();
    }

    public String getString(String name) {
        return new PersistentString(name).get();
    }

    private Map<Class<?>, State<? extends Enum<?>>> states = new HashMap<Class<?>, State<? extends Enum<?>>>();

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

    @SuppressWarnings("unchecked")
    public <T extends Enum<T>> State<T> state(T[] values) {
        Class<Enum<?>> enumClass = (Class<Enum<?>>) values[0].getClass();
        return state(enumClass);
    }

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
     * Get values for any enumeration. This is different from toys and clothing
     * in that those are usually handled by the host.
     * 
     * @param values
     * @return
     */
    public List<Item> get(Enum<? extends Enum<?>>... values) {
        List<Item> items = new ArrayList<Item>(values.length);
        for (Enum<?> v : values) {
            items.add(new Item(v.getClass().getName() + "." + v.name(), v
                    .toString(), persistence));
        }
        return items;
    }
}
