package teaselib.core;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.Actor;
import teaselib.Config;
import teaselib.State;
import teaselib.core.devices.DeviceFactoryListener;
import teaselib.core.media.MediaRendererQueue;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.PropertyNameMapping;
import teaselib.motiondetection.MotionDetection;
import teaselib.motiondetection.MotionDetector;
import teaselib.util.Item;
import teaselib.util.Items;
import teaselib.util.Logger;
import teaselib.util.TextVariables;

public class TeaseLib {
    private static final String CLOTHINGSPACE = "clothes";

    private static final String TOYSPACE = "toys";

    private static final File transcriptLogFile = new File(
            "./TeaseLib session transcript.log");

    public final Host host;
    private final Persistence persistence;
    public final Logger transcript;
    private final Map<Class<?>, StateMap<? extends Enum<?>>> states = new HashMap<Class<?>, StateMap<? extends Enum<?>>>();
    final MediaRendererQueue renderQueue = new MediaRendererQueue();

    public TeaseLib(Host host, Persistence persistence) {
        if (host == null || persistence == null) {
            throw new IllegalArgumentException();
        }
        this.host = host;
        this.persistence = persistence;
        Logger transcriptLogger = null;
        try {
            transcriptLogger = new Logger(transcriptLogFile,
                    getConfigSetting(Config.Debug.LogDetails)
                            ? Logger.Level.Debug : Logger.Level.Info)
                                    .showTime(false).showThread(false);
        } catch (IOException e) {
            host.show(null, "Cannot open log file "
                    + transcriptLogFile.getAbsolutePath());
            host.reply(Arrays.asList("Oh dear"));
            transcriptLogger = Logger.getDummyLogger();
        }
        this.transcript = transcriptLogger;
        MotionDetection.Devices
                .addDeviceListener(new DeviceFactoryListener<MotionDetector>() {
                    @Override
                    public void deviceCreated(MotionDetector motionDetector) {
                        motionDetector.setVideoRenderer(TeaseLib.this.host
                                .getDisplay(VideoRenderer.Type.CameraFeedback));
                    }
                });
    }

    @SuppressWarnings("resource")
    public static void run(Host host, Persistence persistence, File classPath,
            String script) throws ReflectiveOperationException, IOException {
        URLClassLoader classLoader = (URLClassLoader) ClassLoader
                .getSystemClassLoader();
        Class<?> classLoaderClass = URLClassLoader.class;
        try {
            Method method = classLoaderClass.getDeclaredMethod("addURL",
                    new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(classLoader,
                    new Object[] { classPath.toURI().toURL() });
        } catch (IOException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new ReflectiveOperationException(
                    "Error, could not add URL to system classloader");
        }
        new TeaseLib(host, persistence).run(script);
    }

    public static void run(Host host, Persistence persistence, String script)
            throws ReflectiveOperationException {
        new TeaseLib(host, persistence).run(script);
    }

    public void run(String script) throws ReflectiveOperationException {
        Class<?> scriptClass = getClass().getClassLoader().loadClass(script);
        Constructor<?> teaseLibConstructor = scriptClass
                .getConstructor(TeaseLib.class);
        Runnable runnable = (Runnable) teaseLibConstructor.newInstance(this);
        try {
            runnable.run();
        } finally {
            host.show(null, "");
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
    @Deprecated
    public long getTime() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * @return time since midnight 1.1.1970 UTC
     */
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
        public final long startSeconds;

        public Duration() {
            this.startSeconds = getTime(TimeUnit.SECONDS);
        }

        public Duration(long startSeconds) {
            this.startSeconds = startSeconds;
        }

        public long elapsed(TimeUnit unit) {
            long now = getTime(TimeUnit.SECONDS);
            long elapsedSeconds = now - startSeconds;
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

    protected abstract class PersistentValue<T> {
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

        public abstract PersistentValue<T> defaultValue(T value);

        public abstract T value();

        public abstract PersistentValue<T> set(T value);
    }

    /**
     * @author someone
     * 
     *         A persistent boolean value, start value is false
     */
    public class PersistentBoolean extends PersistentValue<Boolean> {
        public final static boolean DefaultValue = false;

        private boolean defaultValue = DefaultValue;

        public PersistentBoolean(String namespace, String name) {
            super(namespace, name);
        }

        public PersistentBoolean(String namespace, Enum<?> name) {
            super(namespace, name);
        }

        @Override
        public PersistentBoolean defaultValue(Boolean defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        public Boolean value() {
            if (persistence.has(name)) {
                return persistence.getBoolean(name);
            } else {
                return defaultValue;
            }
        }

        public void set() {
            set(true);
        }

        @Override
        public PersistentValue<Boolean> set(Boolean value) {
            persistence.set(name, value);
            return this;
        }

        public boolean isTrue() {
            return value() == true;
        }

        public boolean isFalse() {
            return value() == false;
        }
    }

    /**
     * @author someone
     * 
     *         A persistent integer value, start value is 0
     */
    public class PersistentInteger extends PersistentValue<Integer> {
        public final static int DefaultValue = 0;

        private int defaultValue = DefaultValue;

        public PersistentInteger(String namespace, String name) {
            super(namespace, name);
        }

        public PersistentInteger(String namespace, Enum<?> name) {
            super(namespace, name);
        }

        @Override
        public PersistentInteger defaultValue(Integer defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        public Integer value() {
            String value = persistence.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }

        @Override
        public PersistentValue<Integer> set(Integer value) {
            persistence.set(name, Integer.toString(value));
            return this;
        }
    }

    /**
     * @author someone
     * 
     *         A persistent long value, default value is 0.
     *         <p>
     *         The long value can be used to store dates and time.
     */
    public class PersistentLong extends PersistentValue<Long> {
        public final static long DefaultValue = 0;

        private long defaultValue = DefaultValue;

        public PersistentLong(String namespace, String name) {
            super(namespace, name);
        }

        public PersistentLong(String namespace, Enum<?> name) {
            super(namespace, name);
        }

        @Override
        public PersistentLong defaultValue(Long defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        public Long value() {
            String value = persistence.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }

        @Override
        public PersistentValue<Long> set(Long value) {
            persistence.set(name, Long.toString(value));
            return this;
        }
    }

    /**
     * @author someone
     * 
     *         A persistent float value, start value is 0.0
     */
    public class PersistentFloat extends PersistentValue<Double> {
        public final static double DefaultValue = 0.0;

        private double defaultValue = DefaultValue;

        public PersistentFloat(String namespace, String name) {
            super(namespace, name);
        }

        public PersistentFloat(String namespace, Enum<?> name) {
            super(namespace, name);
        }

        @Override
        public PersistentFloat defaultValue(Double defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        public Double value() {
            String value = persistence.get(name);
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

        @Override
        public PersistentValue<Double> set(Double value) {
            persistence.set(name, Double.toString(value));
            return this;
        }
    }

    /**
     * @author someone
     * 
     *         A persistent String value, start value is the empty string
     */
    public class PersistentString extends PersistentValue<String> {
        public final static String DefaultValue = "";

        private String defaultValue = DefaultValue;

        public PersistentString(String namespace, String name) {
            super(namespace, name);
        }

        public PersistentString(String namespace, Enum<?> name) {
            super(namespace, name);
        }

        @Override
        public PersistentString defaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        public String value() {
            String value = persistence.get(name);
            if (value == null) {
                return defaultValue;
            } else {
                return value;
            }
        }

        @Override
        public PersistentValue<String> set(String value) {
            persistence.set(name, value);
            return this;
        }
    }

    public class PersistentEnum<T extends Enum<?>> extends PersistentValue<T> {

        T[] values;
        private T defaultValue = null;

        public PersistentEnum(String namespace, String name, T[] values) {
            super(namespace, name);
            this.values = values;
            defaultValue = values[0];
        }

        public PersistentEnum(String namespace, Enum<?> name, T[] values) {
            super(namespace, name);
            defaultValue = values[0];
        }

        @Override
        public PersistentEnum<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        @Override
        public T value() {
            String valueAsString = persistence.get(name);
            T any = values[0];
            @SuppressWarnings({ "unchecked", "static-access" })
            T value = (T) any.valueOf(any.getClass(), valueAsString);
            if (value == null) {
                return defaultValue;
            } else {
                return value;
            }
        }

        @Override
        public PersistentValue<T> set(T value) {
            persistence.set(name, value.name());
            return this;
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

    public void set(String namespace, Enum<?> name, long value) {
        new PersistentLong(namespace, name).set(value);
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

    public void set(String namespace, String name, long value) {
        new PersistentLong(namespace, name).set(value);
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
        return new PersistentFloat(namespace, name).value();
    }

    public int getInteger(String namespace, String name) {
        return new PersistentInteger(namespace, name).value();
    }

    public long getLong(String namespace, String name) {
        return new PersistentLong(namespace, name).value();
    }

    public String getString(String namespace, String name) {
        return persistence.get(makePropertyName(namespace, name));
    }

    public boolean getBoolean(String namespace, Enum<?> name) {
        return persistence.getBoolean(makePropertyName(namespace, name));
    }

    public double getFloat(String namespace, Enum<?> name) {
        return new PersistentFloat(namespace, name).value();
    }

    public int getInteger(String namespace, Enum<?> name) {
        return new PersistentInteger(namespace, name).value();
    }

    public long getLong(String namespace, Enum<?> name) {
        return new PersistentInteger(namespace, name).value();
    }

    public String getString(String namespace, Enum<?> name) {
        return persistence.get(makePropertyName(namespace, name));
    }

    private String makePropertyName(String namespace, String name) {
        PropertyNameMapping nameMapping = persistence.getNameMapping();
        String mappedDomain = PropertyNameMapping.None;
        String mappedPath = nameMapping.mapPath(PropertyNameMapping.None,
                namespace, name);
        String mappedName = nameMapping.mapName(PropertyNameMapping.None,
                namespace, name);
        return nameMapping.buildPath(mappedDomain, mappedPath, mappedName);
    }

    private String makePropertyName(String namespace, Enum<?> name) {
        // boolean noClassName = name instanceof Toys || name instanceof
        // Clothes;
        // if (noClassName && (TOYSPACE.equals(namespace)
        // || CLOTHINGSPACE.equals(namespace))) {
        // return namespace.toLowerCase() + "." + name.name().toLowerCase();
        // } else {
        // return namespace.toLowerCase() + "."
        // + name.getClass().getSimpleName().toLowerCase() + "."
        // + name.name().toLowerCase();
        // }
        return makePropertyName(namespace, name.name());
    }

    public <T extends Enum<?>> Item<T> getToy(T toy) {
        return item(TOYSPACE, toy);
    }

    public <T extends Enum<?>> Item<T> getClothing(Object wearer, T item) {
        return item(wearer.toString(), item);
    }

    public <T> Item<T> getToy(T toy) {
        return item(TOYSPACE, toy);
    }

    public <T> Item<T> getClothing(Object wearer, T item) {
        return item(wearer.toString(), item);
    }

    public TextVariables getTextVariables(Locale locale) {
        return persistence.getTextVariables(locale);
    }

    public class PersistentSequence<T extends Enum<T>> {
        public final PersistentString valueName;
        public final T[] values;
        private T value;

        public PersistentSequence(String namespace, String name, T[] values) {
            this.valueName = new PersistentString(namespace, name);
            this.values = values;
            String persistedValue = valueName.value();
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
    public <T extends Enum<T>> State state(T item) {
        Class<T> enumClass = (Class<T>) item.getClass();
        StateMap<T> stateMap = state(enumClass);
        State state;
        if (stateMap.has(item)) {
            state = stateMap.get(item);
        } else {
            // If there is no entry, we can assume that the item hasn't been
            // used yet, or no duration was applied
            // -> mark the item as not applied, and taken off a long time ago
            // -> checks against elapsed() do always succeed in this case,
            // allowing for simpler coding on the application layer
            state = stateMap.add(item, 0, 0);
        }
        return state;
    }

    /**
     * Return the state for all or a subset of members of an enumeration.
     * 
     * @param values
     *            The values to retrieve the state for. This should be
     *            {@code Enum.values()}, as the state will only contain the
     *            state items for the listed values.
     * @return The state of all members in {@code values}.
     */
    @SuppressWarnings("unchecked")
    <T extends Enum<T>> StateMap<T> state(T[] values) {
        Class<T> enumClass = (Class<T>) values[0].getClass();
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
    private <T extends Enum<T>> StateMap<T> state(Class<T> enumClass) {
        final StateMap<T> state;
        if (states.containsKey(enumClass)) {
            state = (StateMap<T>) states.get(enumClass);
        } else {
            state = new StateMap<T>(this, enumClass);
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
        return new Item<T>(value, new PersistentBoolean(namespace, value));
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
        return new Item<T>(value,
                new PersistentBoolean(namespace, value.toString()));
    }

    public Actor getDominant(Voice.Gender gender, Locale locale) {
        return persistence.getDominant(gender, locale);
    }

    public boolean getConfigSetting(Enum<?> name) {
        String systemProperty = System.getProperty(
                Config.Namespace + "." + name.toString().toLowerCase(),
                "false");
        boolean teaseLibProperty = getBoolean(Config.Namespace, name);
        boolean ignoreMissingResources = teaseLibProperty
                && systemProperty != "false";
        return ignoreMissingResources;
    }

    public String getConfigString(Enum<?> name) {
        String teaseLibProperty = getString(Config.Namespace, name.toString());
        if (teaseLibProperty.isEmpty()) {
            String systemProperty = System.getProperty(
                    Config.Namespace + "." + name.toString().toLowerCase(), "");
            return systemProperty;
        }
        return teaseLibProperty;
    }
}
