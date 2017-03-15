package teaselib.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Clothes;
import teaselib.Config;
import teaselib.State;
import teaselib.Toys;
import teaselib.core.devices.DeviceFactoryListener;
import teaselib.core.devices.remote.LocalNetworkDevice;
import teaselib.core.devices.remote.LocalNetworkDeviceDiscovery;
import teaselib.core.media.MediaRendererQueue;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.PropertyNameMapping;
import teaselib.core.util.ReflectionUtils;
import teaselib.motiondetection.MotionDetection;
import teaselib.motiondetection.MotionDetector;
import teaselib.util.Item;
import teaselib.util.Items;
import teaselib.util.PersistenceLogger;
import teaselib.util.TeaseLibLogger;
import teaselib.util.TextVariables;

public class TeaseLib {
    private static final Logger logger = LoggerFactory
            .getLogger(TeaseLib.class);

    public static final String DefaultDomain = PropertyNameMapping.DefaultDomain;
    public static final String DefaultName = PropertyNameMapping.None;

    private static final File transcriptLogFile = new File(
            "./TeaseLib session transcript.log");

    public final Host host;
    private final Persistence persistence;
    public final TeaseLibLogger transcript;
    private final Map<Class<?>, StateMap<? extends Enum<?>>> stateMaps = new HashMap<Class<?>, StateMap<? extends Enum<?>>>();
    final MediaRendererQueue renderQueue = new MediaRendererQueue();

    public TeaseLib(Host host, Persistence persistence) {
        if (host == null || persistence == null) {
            throw new IllegalArgumentException();
        }
        this.host = host;
        this.persistence = new PersistenceLogger(persistence);
        this.transcript = newTranscriptLogger(host);
        bindMotionDetectorFeedback();
        if (LocalNetworkDeviceDiscovery
                .isListeningForDeviceStartupMessagesEnabled()) {
            LocalNetworkDevice.startDeviceDetection();
        }
    }

    private TeaseLibLogger newTranscriptLogger(Host host) {
        TeaseLibLogger transcriptLogger = null;
        try {
            transcriptLogger = new TeaseLibLogger(transcriptLogFile,
                    getConfigSetting(Config.Debug.LogDetails)
                            ? TeaseLibLogger.Level.Debug
                            : TeaseLibLogger.Level.Info).showTime(false)
                                    .showThread(false);
        } catch (IOException e) {
            host.show(null, "Cannot open log file "
                    + transcriptLogFile.getAbsolutePath());
            host.reply(Arrays.asList("Oh dear"));
            transcriptLogger = TeaseLibLogger.getDummyLogger();
        }
        return transcriptLogger;
    }

    private void bindMotionDetectorFeedback() {
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
        if (!classPath.exists()) {
            throw new FileNotFoundException(classPath.getAbsolutePath());
        }
        host.showInterTitle("");
        URLClassLoader classLoader = (URLClassLoader) ClassLoader
                .getSystemClassLoader();
        Class<?> classLoaderClass = URLClassLoader.class;
        try {
            Method method = classLoaderClass.getDeclaredMethod("addURL",
                    new Class[] { URL.class });
            method.setAccessible(true);
            method.invoke(classLoader,
                    new Object[] { classPath.toURI().toURL() });
            logger.info("Added class path " + classPath.getAbsolutePath());
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
        run(host, persistence, script);
    }

    private static void run(Host host, Persistence persistence, String script)
            throws ReflectiveOperationException {
        new TeaseLib(host, persistence).run(script);
    }

    private void run(String script) throws ReflectiveOperationException {
        logger.info("Running script " + script);
        Class<?> scriptClass = getClass().getClassLoader().loadClass(script);
        run(scriptClass);
    }

    private void run(Class<?> scriptClass)
            throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        Constructor<?> mainscriptConstructor = scriptClass
                .getConstructor(TeaseLib.class);
        Runnable runnable = (Runnable) mainscriptConstructor.newInstance(this);
        run(runnable);
    }

    private void run(Runnable script) {
        try {
            script.run();
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
        return teaselib.util.math.Random.random(min, max);
    }

    public double random(double min, double max) {
        return teaselib.util.math.Random.random(min, max);
    }

    /**
     * @author Citizen-Cane
     * 
     *         Handles elapsed seconds since object creation
     */
    public class Duration {
        private final TimeUnit DURATION_TIME_UNIT = TimeUnit.SECONDS;

        private final long start;
        private final long limit;

        public Duration() {
            this(0, TimeUnit.SECONDS);
        }

        public Duration(long limit, TimeUnit unit) {
            this.start = getTime(DURATION_TIME_UNIT);
            this.limit = DURATION_TIME_UNIT.convert(limit, unit);
        }

        Duration(long startSeconds) {
            this.start = DURATION_TIME_UNIT.convert(startSeconds,
                    TimeUnit.SECONDS);
            this.limit = 0;
        }

        public long start(TimeUnit unit) {
            return unit.convert(start, TimeUnit.MILLISECONDS);
        }

        public long elapsed(TimeUnit unit) {
            long now = getTime(DURATION_TIME_UNIT);
            long elapsed = now - start;
            return unit.convert(elapsed, DURATION_TIME_UNIT);
        }
        public boolean expired() {
            return getTime(DURATION_TIME_UNIT) - start >= limit;
        }
    }

    protected abstract class PersistentValue<T> {
        public final String name;
        protected T defaultValue;

        protected PersistentValue(String domain, String namespace, String name,
                T defaultValue) {
            this.name = makePropertyName(domain, namespace, name);
            this.defaultValue = defaultValue;
        }

        protected PersistentValue(String domain, Enum<?> name, T defaultValue) {
            this.name = makePropertyName(domain, name);
            this.defaultValue = defaultValue;
        }

        public void clear() {
            persistence.getNameMapping().clear(name, persistence);
        }

        public boolean available() {
            return persistence.getNameMapping().has(name, persistence);
        }

        public PersistentValue<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public T defaultValue() {
            return defaultValue;
        }

        public abstract T value();

        public abstract PersistentValue<T> set(T value);
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent boolean value, start value is false
     */
    public class PersistentBoolean extends PersistentValue<Boolean> {
        public final static boolean DefaultValue = false;

        public PersistentBoolean(String domain, String namespace, String name) {
            super(domain, namespace, name, DefaultValue);
        }

        public PersistentBoolean(String domain, Enum<?> name) {
            super(domain, name, DefaultValue);
        }

        @Override
        public PersistentBoolean defaultValue(Boolean defaultValue) {
            return (PersistentBoolean) super.defaultValue(defaultValue);
        }

        @Override
        public Boolean value() {
            if (persistence.getNameMapping().has(name, persistence)) {
                return persistence.getNameMapping().getBoolean(name,
                        persistence);
            } else {
                return defaultValue;
            }
        }

        public void set() {
            set(true);
        }

        @Override
        public PersistentValue<Boolean> set(Boolean value) {
            persistence.getNameMapping().set(name, value, persistence);
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
     * @author Citizen-Cane
     * 
     *         A persistent integer value, start value is 0
     */
    public class PersistentInteger extends PersistentValue<Integer> {
        public final static int DefaultValue = 0;

        public PersistentInteger(String domain, String namespace, String name) {
            super(domain, namespace, name, DefaultValue);
        }

        public PersistentInteger(String domain, Enum<?> name) {
            super(domain, name, DefaultValue);
        }

        @Override
        public PersistentInteger defaultValue(Integer defaultValue) {
            return (PersistentInteger) super.defaultValue(defaultValue);
        }

        @Override
        public Integer value() {
            String value = persistence.getNameMapping().get(name, persistence);
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
            persistence.getNameMapping().set(name, Integer.toString(value),
                    persistence);
            return this;
        }
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent long value, default value is 0.
     *         <p>
     *         The long value can be used to store dates and time.
     */
    public class PersistentLong extends PersistentValue<Long> {
        public final static long DefaultValue = 0;

        public PersistentLong(String domain, String namespace, String name) {
            super(domain, namespace, name, DefaultValue);
        }

        public PersistentLong(String domain, Enum<?> name) {
            super(domain, name, DefaultValue);
        }

        @Override
        public PersistentLong defaultValue(Long defaultValue) {
            return (PersistentLong) super.defaultValue(defaultValue);
        }

        @Override
        public Long value() {
            String value = persistence.getNameMapping().get(name, persistence);
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
            persistence.getNameMapping().set(name, Long.toString(value),
                    persistence);
            return this;
        }
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent float value, start value is 0.0
     */
    public class PersistentFloat extends PersistentValue<Double> {
        public final static double DefaultValue = 0.0;

        public PersistentFloat(String domain, String namespace, String name) {
            super(domain, namespace, name, DefaultValue);
        }

        public PersistentFloat(String domain, Enum<?> name) {
            super(domain, name, DefaultValue);
        }

        @Override
        public PersistentFloat defaultValue(Double defaultValue) {
            return (PersistentFloat) super.defaultValue(defaultValue);
        }

        @Override
        public Double value() {
            String value = persistence.getNameMapping().get(name, persistence);
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
            persistence.getNameMapping().set(name, Double.toString(value),
                    persistence);
            return this;
        }
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent String value, start value is the empty string
     */
    public class PersistentString extends PersistentValue<String> {
        public final static String DefaultValue = "";

        public PersistentString(String domain, String namespace, String name) {
            super(domain, namespace, name, DefaultValue);
        }

        public PersistentString(String domain, Enum<?> name) {
            super(domain, name, DefaultValue);
        }

        @Override
        public PersistentString defaultValue(String defaultValue) {
            return (PersistentString) super.defaultValue(defaultValue);
        }

        @Override
        public String value() {
            String value = persistence.getNameMapping().get(name, persistence);
            if (value == null) {
                return defaultValue;
            } else {
                return value;
            }
        }

        @Override
        public PersistentValue<String> set(String value) {
            persistence.getNameMapping().set(name, value, persistence);
            return this;
        }
    }

    public class PersistentEnum<T extends Enum<?>> extends PersistentValue<T> {

        public PersistentEnum(String domain, Class<T> enumClass) {
            super(domain, ReflectionUtils.classParentName(enumClass),
                    ReflectionUtils.classSimpleName(enumClass),
                    enumClass.getEnumConstants()[0]);
        }

        public PersistentEnum(String domain, String namespace, String name,
                Class<T> enumClass) {
            super(domain, namespace, name, enumClass.getEnumConstants()[0]);
        }

        public PersistentEnum(String domain, Enum<?> name, Class<T> enumClass) {
            super(domain, name.getClass().getName(), DefaultName,
                    enumClass.getEnumConstants()[0]);
        }

        @Override
        public PersistentEnum<T> defaultValue(T defaultValue) {
            return (PersistentEnum<T>) super.defaultValue(defaultValue);
        }

        @Override
        public T value() {
            T any = defaultValue;
            if (persistence.getNameMapping().has(name, persistence)) {
                String valueAsString = persistence.getNameMapping().get(name,
                        persistence);
                @SuppressWarnings({ "unchecked", "static-access" })
                T value = (T) any.valueOf(any.getClass(), valueAsString);
                if (value == null) {
                    return defaultValue;
                } else {
                    return value;
                }
            } else {
                return defaultValue;
            }
        }

        @Override
        public PersistentValue<T> set(T value) {
            persistence.getNameMapping().set(name, value.name(), persistence);
            return this;
        }
    }

    public void clear(String domain, String namespace, String name) {
        persistence.getNameMapping()
                .clear(makePropertyName(domain, namespace, name), persistence);
    }

    public void clear(String domain, Enum<?> name) {
        persistence.getNameMapping().clear(makePropertyName(domain, name),
                persistence);
    }

    public void set(String domain, Enum<?> name, boolean value) {
        persistence.getNameMapping().set(makePropertyName(domain, name), value,
                persistence);
    }

    public void set(String domain, Enum<?> name, int value) {
        new PersistentInteger(domain, name).set(value);
    }

    public void set(String domain, Enum<?> name, long value) {
        new PersistentLong(domain, name).set(value);
    }

    public void set(String domain, Enum<?> name, double value) {
        new PersistentFloat(domain, name).set(value);
    }

    public void set(String domain, Enum<?> name, String value) {
        persistence.getNameMapping().set(makePropertyName(domain, name), value,
                persistence);
    }

    public void set(String domain, String namespace, String name,
            boolean value) {
        persistence.getNameMapping().set(
                makePropertyName(domain, namespace, name), value, persistence);
    }

    public void set(String domain, String namespace, String name, int value) {
        new PersistentInteger(domain, namespace, name).set(value);
    }

    public void set(String domain, String namespace, String name, long value) {
        new PersistentLong(domain, namespace, name).set(value);
    }

    public void set(String domain, String namespace, String name,
            double value) {
        new PersistentFloat(domain, namespace, name).set(value);
    }

    public void set(String domain, String namespace, String name,
            String value) {
        persistence.getNameMapping().set(
                makePropertyName(domain, namespace, name), value, persistence);
    }

    public boolean getBoolean(String domain, String namespace, String name) {
        return persistence.getNameMapping().getBoolean(
                makePropertyName(domain, namespace, name), persistence);
    }

    public double getFloat(String domain, String namespace, String name) {
        return new PersistentFloat(domain, namespace, name).value();
    }

    public int getInteger(String domain, String namespace, String name) {
        return new PersistentInteger(domain, namespace, name).value();
    }

    public long getLong(String domain, String namespace, String name) {
        return new PersistentLong(domain, namespace, name).value();
    }

    public String getString(String domain, String namespace, String name) {
        return persistence.getNameMapping()
                .get(makePropertyName(domain, namespace, name), persistence);
    }

    public boolean getBoolean(String domain, Enum<?> name) {
        return persistence.getNameMapping()
                .getBoolean(makePropertyName(domain, name), persistence);
    }

    public double getFloat(String domain, Enum<?> name) {
        return new PersistentFloat(domain, name).value();
    }

    public int getInteger(String domain, Enum<?> name) {
        return new PersistentInteger(domain, name).value();
    }

    public long getLong(String domain, Enum<?> name) {
        return new PersistentInteger(domain, name).value();
    }

    public String getString(String domain, Enum<?> name) {
        return persistence.getNameMapping().get(makePropertyName(domain, name),
                persistence);
    }

    private String makePropertyName(String domain, String path, String name) {
        PropertyNameMapping nameMapping = persistence.getNameMapping();

        String strippedPath = nameMapping.stripPath(domain, path, name);

        String mappedDomain = nameMapping.mapDomain(domain, strippedPath, name);
        String mappedPath = nameMapping.mapPath(domain, strippedPath, name);
        String mappedName = nameMapping.mapName(domain, strippedPath, name);

        return nameMapping.buildPath(mappedDomain, mappedPath, mappedName);
    }

    private String makePropertyName(String domain, Enum<?> name) {
        return makePropertyName(domain, name.getClass().getName(), name.name());
    }

    @Deprecated
    public <T extends Enum<?>> Item<T> getToy(String domain, T toy) {
        return item(domain, toy);
    }

    @Deprecated
    public Item<String> getToy(String domain, String toy) {
        return item(domain, Toys.class.getName(), toy);
    }

    @Deprecated
    public <T extends Enum<?>> Item<T> getClothing(Object domain, T item) {
        return item(domain.toString(), item);
    }

    @Deprecated
    public Item<String> getClothing(Object domain, String item) {
        return item(domain.toString(), Clothes.class.getName(), item);
    }

    public TextVariables getTextVariables(Locale locale) {
        return persistence.getTextVariables(locale);
    }

    public class PersistentSequence<T extends Enum<T>> {
        public final PersistentString valueName;
        public final T[] values;
        private T value;

        public PersistentSequence(String domain, String namespace, String name,
                T[] values) {
            this.valueName = new PersistentString(domain, namespace, name);
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
        return state((Class<T>) item.getClass()).get(item);
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
        return state((Class<T>) values[0].getClass());
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
        final StateMap<T> stateMap;
        if (stateMaps.containsKey(enumClass)) {
            stateMap = (StateMap<T>) stateMaps.get(enumClass);
        } else {
            stateMap = new StateMap<T>(this);
            stateMaps.put(enumClass, stateMap);
        }
        return stateMap;
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
    public <T extends Enum<?>> Items<T> items(String domain, String namespace,
            T... values) {
        Items<T> items = new Items<T>(values.length);
        for (T v : values) {
            items.add(item(domain, v));
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
    public <T extends Enum<?>> Item<T> item(String domain, T value) {
        return new Item<T>(value, new PersistentBoolean(domain,
                value.getClass().getName(), value.name()));
    }

    /**
     * Get the item for any object.
     * 
     * @param namespace
     *            The namespace of the item.
     * @param name
     *            The value to get the item for.
     * @return The item that corresponds to the value.
     */
    public Item<String> item(String domain, String namespace, String name) {
        return new Item<String>(name,
                new PersistentBoolean(domain, namespace, name));
    }

    public Actor getDominant(Voice.Gender gender, Locale locale) {
        return persistence.getDominant(gender, locale);
    }

    public boolean getConfigSetting(Enum<?> name) {
        String systemProperty = System
                .getProperty(Config.Namespace + "." + name.toString(), "false");
        boolean teaseLibProperty = getBoolean(TeaseLib.DefaultDomain, name);
        boolean finalProperty = teaseLibProperty && systemProperty != "false";
        return finalProperty;
    }

    public String getConfigString(Enum<?> name) {
        String teaseLibProperty = getString(TeaseLib.DefaultDomain,
                Config.Namespace, name.toString());
        if (teaseLibProperty.isEmpty()) {
            String systemProperty = System
                    .getProperty(Config.Namespace + "." + name.toString(), "");
            return systemProperty;
        }
        return teaseLibProperty;
    }
}
