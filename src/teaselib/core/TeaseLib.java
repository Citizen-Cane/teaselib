package teaselib.core;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.Config;
import teaselib.Duration;
import teaselib.Sexuality.Gender;
import teaselib.State;
import teaselib.core.Host.Location;
import teaselib.core.StateMaps.StateMapCache;
import teaselib.core.configuration.Configuration;
import teaselib.core.configuration.Setup;
import teaselib.core.configuration.TeaseLibConfigSetup;
import teaselib.core.debug.CheckPoint;
import teaselib.core.debug.CheckPointListener;
import teaselib.core.debug.TimeAdvanceListener;
import teaselib.core.debug.TimeAdvancedEvent;
import teaselib.core.devices.Devices;
import teaselib.core.state.AbstractProxy;
import teaselib.core.util.ConfigFileMapping;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.ObjectMap;
import teaselib.core.util.QualifiedEnum;
import teaselib.core.util.QualifiedItem;
import teaselib.core.util.QualifiedName;
import teaselib.core.util.ReflectionUtils;
import teaselib.functional.RunnableScript;
import teaselib.util.Daytime;
import teaselib.util.Item;
import teaselib.util.ItemGuid;
import teaselib.util.ItemImpl;
import teaselib.util.Items;
import teaselib.util.TeaseLibLogger;
import teaselib.util.TextVariables;
import teaselib.util.math.Random;

public class TeaseLib implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLib.class);

    public static final String DefaultDomain = "";
    public static final String DefaultName = "";

    private static final String TranscriptLogFileName = "TeaseLib session transcript.log";

    public final Host host;
    final Persistence persistence;
    final UserItems userItems;
    public final TeaseLibLogger transcript;

    public final Configuration config;
    public final ObjectMap globals = new ObjectMap();
    final StateMaps stateMaps;
    public final Devices devices;
    public final Random random;

    private final AtomicReference<Thread> timeAdvanceThread = new AtomicReference<>(null);
    private final AtomicLong frozenTime = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong timeOffsetMillis = new AtomicLong(0);
    private final Set<TimeAdvanceListener> timeAdvanceListeners = new HashSet<>();
    private final Set<CheckPointListener> checkPointListeners = new HashSet<>();

    public TeaseLib(final Host host, Persistence persistence) throws IOException {
        this(host, persistence, new TeaseLibConfigSetup(host));
    }

    public TeaseLib(Host host, Persistence persistence, Setup setup) throws IOException {
        if (host == null || persistence == null || setup == null) {
            throw new IllegalArgumentException();
        }

        logDateTime();
        logJavaVersion();
        logJavaProperties();

        this.config = new Configuration(setup);
        this.host = host;
        this.persistence = new ConfigFileMapping(config, persistence);

        this.userItems = persistence.getUserItems(this);
        this.transcript = newTranscriptLogger(host.getLocation(Location.Log));

        this.stateMaps = new StateMaps(this);
        this.devices = new Devices(config);

        this.random = new Random();
    }

    private static void logDateTime() {
        logger.info("{}", new Date(System.currentTimeMillis()));
    }

    private static void logJavaProperties() {
        for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
            logger.debug("{}={}", entry.getKey(), entry.getValue());
        }
    }

    private static void logJavaVersion() {
        if (logger.isInfoEnabled()) {
            StringBuilder javaVersion = getJavaVersionString();
            logger.info(javaVersion.toString());
        }
    }

    private static StringBuilder getJavaVersionString() {
        Set<String> javaProperties = new LinkedHashSet<>(
                Arrays.asList("java.vm.name", "java.runtime.version", "os.name", "os.arch"));

        var javaVersion = new StringBuilder();
        for (String name : javaProperties) {
            String property = System.getProperties().getProperty(name);
            if (property != null) {
                if (javaVersion.length() > 0)
                    javaVersion.append(" ");
                javaVersion.append(property);
            }
        }
        return javaVersion;
    }

    private TeaseLibLogger newTranscriptLogger(File folder) throws IOException {
        TeaseLibLogger transcriptLogger = null;

        transcriptLogger = new TeaseLibLogger(new File(folder, TranscriptLogFileName),
                Boolean.parseBoolean(config.get(Config.Debug.LogDetails)) ? TeaseLibLogger.Level.Debug
                        : TeaseLibLogger.Level.Info).showTime(false).showThread(false);
        return transcriptLogger;
    }

    public static void run(Host host, Persistence persistence, File scriptClassPath, String script)
            throws IOException, ReflectiveOperationException {
        run(host, persistence, scriptClassPath, new TeaseLibConfigSetup(host), script);
    }

    public static void run(Host host, Persistence persistence, File scriptClassPath, Setup setup, String script)
            throws ReflectiveOperationException, IOException {
        if (!scriptClassPath.exists()) {
            throw new FileNotFoundException(scriptClassPath.getAbsolutePath());
        }

        try (URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();) {
            Class<URLClassLoader> classLoaderClass = URLClassLoader.class;
            var method = classLoaderClass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, scriptClassPath.toURI().toURL());
            logger.info("Added class path {}", scriptClassPath.getAbsolutePath());
            run(host, persistence, setup, script);
        }
    }

    public static void run(Host host, Persistence persistence, Setup setup, String script) throws IOException {
        try (var teaseLib = new TeaseLib(host, persistence, setup)) {
            teaseLib.run(script);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    private void run(String scriptName) throws ReflectiveOperationException {
        host.showInterTitle("");
        host.show();
        try {
            logger.info("Running script {}", scriptName);
            var contextClassLoader = Thread.currentThread().getContextClassLoader();
            @SuppressWarnings("unchecked")
            Class<RunnableScript> scriptClass = (Class<RunnableScript>) contextClassLoader.loadClass(scriptName);
            RunnableScript script = script(scriptClass);
            script.run();
        } finally {
            try {
                temporaryItems().remove();
            } catch (Throwable ignored) {
                logger.warn(ignored.getMessage(), ignored);
            }
        }
        host.show(null, Collections.emptyList());
        host.show();
    }

    static class MainScriptConstructorMissingException extends NoSuchMethodException {
        public MainScriptConstructorMissingException(NoSuchMethodException e) {
            super("Missing mainscript constructor " + e.getMessage());
        }

        private static final long serialVersionUID = 1L;
    }

    private RunnableScript script(Class<RunnableScript> scriptClass) throws ReflectiveOperationException {
        Constructor<?> mainscriptConstructor;
        try {
            mainscriptConstructor = scriptClass.getDeclaredConstructor(TeaseLib.class);
        } catch (NoSuchMethodException e) {
            throw new MainScriptConstructorMissingException(e);
        }
        try {
            return (RunnableScript) mainscriptConstructor.newInstance(this);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.reduced(e);
        }
    }

    @Override
    public void close() {
        boolean isInterrupted = Thread.interrupted();
        try {
            globals.close();
            if (host instanceof Closeable) {
                ((Closeable) host).close();
            }
        } finally {
            if (isInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Preferred method to wait, since it allows us to test script with automated input and time advance.
     * 
     * @param milliseconds
     *            The time to sleep.
     * @throws ScriptInterruptedException
     */
    public void sleep(long duration, TimeUnit unit) {
        if (duration > 0) {
            if (isTimeFrozen()) {
                sleepWhileTimeIsFrozen(duration, unit);
            } else {
                sleepInRealTime(duration, unit);
            }
        }
    }

    private void sleepWhileTimeIsFrozen(long duration, TimeUnit unit) {
        if (Thread.interrupted()) {
            throw new ScriptInterruptedException();
        }

        if (timeAdvanceThread.get() == null || timeAdvanceThread.get() == Thread.currentThread()) {
            advanceTime(duration, unit);
        } else {
            advanceTime(0, unit);
        }
        fireTimeAdvanced();

        if (Thread.interrupted()) {
            throw new ScriptInterruptedException();
        }
    }

    private void sleepInRealTime(long duration, TimeUnit unit) {
        try {
            if (timeAdvanceListeners.isEmpty()) {
                unit.sleep(duration);
            } else {
                long milliSeconds = duration == Long.MAX_VALUE ? Long.MAX_VALUE : MILLISECONDS.convert(duration, unit);
                while (milliSeconds > 0) {
                    TimeUnit.MILLISECONDS.sleep(Math.min(1000, milliSeconds));
                    fireTimeAdvanced();
                    milliSeconds -= 1000;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptInterruptedException(e);
        }

        fireTimeAdvanced();
    }

    void addTimeAdvancedListener(TimeAdvanceListener listener) {
        timeAdvanceListeners.add(listener);
    }

    void removeTimeAdvancedListener(TimeAdvanceListener listener) {
        timeAdvanceListeners.remove(listener);
    }

    private void fireTimeAdvanced() {
        for (TimeAdvanceListener timeAdvancedListener : timeAdvanceListeners) {
            timeAdvancedListener.timeAdvanced(new TimeAdvancedEvent(this));
        }
    }

    void addCheckPointListener(CheckPointListener listener) {
        checkPointListeners.add(listener);
    }

    void removeCheckPointListener(CheckPointListener listener) {
        checkPointListeners.remove(listener);
    }

    public void checkPointReached(CheckPoint checkPoint) {
        if (!checkPointListeners.isEmpty()) {
            logger.info("Checkpoint {}", checkPoint);
            for (CheckPointListener checkPointListener : checkPointListeners) {
                checkPointListener.checkPointReached(checkPoint);
            }
        }
    }

    /**
     * @return time since midnight 1.1.1970 UTC
     */
    public long getTime(TimeUnit unit) {
        final long time;
        if (isTimeFrozen()) {
            time = frozenTime.get() + timeOffsetMillis.get();
        } else {
            long now = System.currentTimeMillis();
            time = now + timeOffsetMillis.get();
        }
        return unit.convert(time, TimeUnit.MILLISECONDS);
    }

    boolean isTimeFrozen() {
        return frozenTime.get() > Long.MIN_VALUE;
    }

    void freezeTime() {
        if (!isTimeFrozen()) {
            frozenTime.set(getTime(TimeUnit.MILLISECONDS));
            timeAdvanceThread.set(Thread.currentThread());
        }
    }

    void advanceTimeAllThreads() {
        timeAdvanceThread.set(null);
    }

    void advanceTime(long duration, TimeUnit unit) {
        if (duration == Long.MAX_VALUE) {
            timeOffsetMillis.set(Long.MAX_VALUE - frozenTime.get());
        } else {
            timeOffsetMillis.addAndGet(unit.toMillis(duration));
        }
    }

    void resumeTime() {
        frozenTime.set(Long.MIN_VALUE);
        timeAdvanceThread.set(null);
    }

    public TimeOfDay timeOfDay() {
        var now = localTime(getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        return new TimeOfDayImpl(now);
    }

    private static LocalTime localTime(long time, TimeUnit unit) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(unit.toMillis(time)), ZoneId.systemDefault()).toLocalTime();
    }

    static final TimeUnit DURATION_TIME_UNIT = TimeUnit.SECONDS;

    public Duration duration() {
        return new DurationImpl(this);
    }

    public Duration duration(long limit, TimeUnit unit) {
        return new DurationImpl(this, limit, unit);
    }

    public Duration duration(Daytime dayTime) {
        return duration(dayTime, 0);
    }

    public Duration duration(Daytime dayTime, long daysInTheFuture) {
        LocalTime start = TimeOfDayImpl.getTime(timeOfDay());
        LocalTime end = localTime((long) TimeOfDayImpl.hours(dayTime).average() * 60L, TimeUnit.MINUTES);

        long durationMinutes = end.getHour() * 60 + end.getMinute() //
                - start.getHour() * 60 - start.getMinute();

        if (durationMinutes < 0) {
            durationMinutes += 24 * 60 * Math.max(1, daysInTheFuture);
        } else {
            durationMinutes += 24 * 60 * daysInTheFuture;
        }

        return duration(durationMinutes, TimeUnit.MINUTES);
    }

    protected abstract class PersistentValue<T> {
        public final QualifiedName name;
        protected T defaultValue;

        protected PersistentValue(String domain, String namespace, String name, T defaultValue) {
            this.name = QualifiedName.of(domain, namespace, name);
            this.defaultValue = defaultValue;
        }

        protected PersistentValue(String domain, Enum<?> item, T defaultValue) {
            this.name = QualifiedName.of(domain, item);
            this.defaultValue = defaultValue;
        }

        public void clear() {
            persistence.clear(name);
        }

        public boolean available() {
            return persistence.has(name);
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

        @Override
        public String toString() {
            return name + "=" + value();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked")
            PersistentValue<T> other = (PersistentValue<T>) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (defaultValue == null) {
                if (other.defaultValue != null)
                    return false;
            } else if (!defaultValue.equals(other.defaultValue))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }

        private TeaseLib getOuterType() {
            return TeaseLib.this;
        }
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent boolean value, start value is false
     */
    public class PersistentBoolean extends PersistentValue<Boolean> {
        public static final boolean DefaultValue = false;

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
            return value();
        }

        public boolean isFalse() {
            return !value();
        }
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent integer value, start value is 0
     */
    public class PersistentInteger extends PersistentValue<Integer> {
        public static final int DefaultValue = 0;

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
     * @author Citizen-Cane
     * 
     *         A persistent long value, default value is 0.
     *         <p>
     *         The long value can be used to store dates and time.
     */
    public class PersistentLong extends PersistentValue<Long> {
        public static final long DefaultValue = 0;

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
     * @author Citizen-Cane
     * 
     *         A persistent float value, start value is 0.0
     */
    public class PersistentFloat extends PersistentValue<Double> {
        public static final double DefaultValue = 0.0;

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
     * @author Citizen-Cane
     * 
     *         A persistent String value, start value is the empty string
     */
    public class PersistentString extends PersistentValue<String> {
        public static final String DefaultValue = "";

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

        public PersistentEnum(String domain, Class<T> enumClass) {
            super(domain, ReflectionUtils.classParentName(enumClass), ReflectionUtils.classSimpleName(enumClass),
                    enumClass.getEnumConstants()[0]);
        }

        public PersistentEnum(String domain, String namespace, String name, Class<T> enumClass) {
            super(domain, namespace, name, enumClass.getEnumConstants()[0]);
        }

        public PersistentEnum(String domain, Enum<?> name, Class<T> enumClass) {
            super(domain, name.getClass().getName(), DefaultName, enumClass.getEnumConstants()[0]);
        }

        @Override
        public PersistentEnum<T> defaultValue(T defaultValue) {
            return (PersistentEnum<T>) super.defaultValue(defaultValue);
        }

        @Override
        public T value() {
            T any = defaultValue;
            if (persistence.has(name)) {
                var valueAsString = persistence.get(name);
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
            persistence.set(name, value.name());
            return this;
        }
    }

    public void clear(String domain, String namespace, String name) {
        persistence.clear(QualifiedName.of(domain, namespace, name));
    }

    public void clear(String domain, Enum<?> name) {
        persistence.clear(QualifiedName.of(domain, name));
    }

    public void set(String domain, Enum<?> name, boolean value) {
        persistence.set(QualifiedName.of(domain, name), value);
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
        persistence.set(QualifiedName.of(domain, name), value);
    }

    public void set(String domain, String namespace, String name, boolean value) {
        persistence.set(QualifiedName.of(domain, namespace, name), value);
    }

    public void set(String domain, String namespace, String name, int value) {
        new PersistentInteger(domain, namespace, name).set(value);
    }

    public void set(String domain, String namespace, String name, long value) {
        new PersistentLong(domain, namespace, name).set(value);
    }

    public void set(String domain, String namespace, String name, double value) {
        new PersistentFloat(domain, namespace, name).set(value);
    }

    public void set(String domain, String namespace, String name, String value) {
        persistence.set(QualifiedName.of(domain, namespace, name), value);
    }

    public boolean getBoolean(String domain, String namespace, String name) {
        return persistence.getBoolean(QualifiedName.of(domain, namespace, name));
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
        return persistence.get(QualifiedName.of(domain, namespace, name));
    }

    public boolean getBoolean(String domain, Enum<?> name) {
        return persistence.getBoolean(QualifiedName.of(domain, name));
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
        return persistence.get(QualifiedName.of(domain, name));
    }

    public TextVariables getTextVariables(String domain, Locale locale) {
        var variables = new TextVariables();
        variables.setUserIdentity(this, domain, locale);
        variables.setAll(persistence.getTextVariables(locale));
        return variables;
    }

    public class PersistentSequence<T extends Enum<T>> {
        public final PersistentString valueName;
        public final T[] values;
        private T value;

        public PersistentSequence(String domain, String namespace, String name, T[] values) {
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
    public State state(String domain, Object item) {
        return stateMaps.state(domain, item);
    }

    public State state(String domain, State state) {
        return stateMaps.state(domain, state);
    }

    /**
     * Get items from a enumeration.
     * 
     * @param domain
     * @param values
     * 
     * @return A list of items whose names are based on the enumeration members
     */
    public Items items(String domain, Object... values) {
        // TODO add some randomness in the order of which items of each kind, but make it session-constant
        // - per default, the fist applied, available, or listed item is used
        // -> shuffle the list per kind according to some session-constant randomness
        // TODO provide a set of default item set by the script or user interface to select items for a session
        List<Item> items = new ArrayList<>();
        for (Object item : values) {
            items.addAll(userItems.get(domain, QualifiedItem.of(item)));
        }
        return new Items(items);
    }

    public Items relatedItems(Enum<?> domain, Items items) {
        return relatedItems(new QualifiedEnum(domain), items);
    }

    public Items relatedItems(QualifiedItem domain, Items items) {
        return relatedItems(domain.toString(), items);
    }

    public Items relatedItems(String domain, Items items) {
        return new Items(items.stream().map(AbstractProxy::itemImpl).map(itemImpl -> getItem(domain, itemImpl))
                .collect(toList()));
    }

    /**
     * @return All temporary items
     */
    Items temporaryItems() {
        List<Item> temporaryItems = new ArrayList<>();
        for (Entry<String, StateMapCache> domains : stateMaps.cache.entrySet()) {
            String domain = domains.getKey();
            for (Entry<String, StateMap> namespace : domains.getValue().entrySet()) {
                for (Entry<Object, State> entries : namespace.getValue().states.entrySet()) {
                    StateImpl state = (StateImpl) entries.getValue();
                    if (!ItemGuid.isGuid(state.item.toString())
                            && state.duration().limit(TimeUnit.SECONDS) == State.TEMPORARY) {
                        temporaryItems.addAll(state.peers().stream().filter(guid -> guid instanceof ItemGuid)
                                .map(ItemGuid.class::cast).map(guid -> getItem(domain, state.item, guid.name()))
                                .collect(Collectors.toList()));
                    }
                }
            }
        }
        return new Items(temporaryItems);
    }

    /**
     * Get the item for any object.
     * 
     * @param namespace
     *            The namespace of the item.
     * @param item
     *            The value to get the item for.
     * @return The item that corresponds to the value.
     */
    public <T extends Object> Item item(String domain, T item) {
        if (item instanceof Item) {
            throw new IllegalArgumentException(item.toString());
        } else {
            return items(domain, item).get();
        }
    }

    public Item getItem(String domain, ItemImpl item) {
        return getItem(domain, item.value, item.guid.name());
    }

    public Item getItem(String domain, Object item, String guid) {
        var match = findItem(domain, item, guid);
        if (match == Item.NotFound) {
            throw new NoSuchElementException(
                    "Item " + QualifiedName.of(domain, QualifiedName.NONE, item.toString()) + ":" + guid);
        } else {
            return match;
        }
    }

    public Item findItem(String domain, Object item, ItemGuid guid) {
        return findItem(domain, item, guid.name());
    }

    public Item findItem(String domain, Object item, String guid) {
        return items(domain, item).stream().filter(i -> ((ItemImpl) i).guid.name().equals(guid)).findFirst()
                .orElse(Item.NotFound);
    }

    public Actor getDominant(Gender gender, Locale locale) {
        return persistence.getDominant(gender, locale);
    }

    public void addUserItems(URL items) {
        userItems.addItems(items);
    }

}
