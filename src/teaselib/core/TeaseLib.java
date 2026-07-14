package teaselib.core;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static teaselib.core.util.ReflectionUtils.classSimpleName;
import static teaselib.core.util.ReflectionUtils.parent;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import teaselib.Actor;
import teaselib.ActorImages;
import teaselib.Config;
import teaselib.Duration;
import teaselib.Sexuality.Gender;
import teaselib.State;
import teaselib.State.Persistence.Until;
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
import teaselib.core.util.QualifiedName;
import teaselib.core.util.QualifiedString;
import teaselib.core.util.ReflectionUtils;
import teaselib.functional.RunnableScript;
import teaselib.host.Host;
import teaselib.host.Host.Location;
import teaselib.host.TeaseLibAudioSystem;
import teaselib.util.Daytime;
import teaselib.util.Item;
import teaselib.util.Items;
import teaselib.util.TeaseLibLogger;
import teaselib.util.TextVariables;
import teaselib.util.math.Random;

public class TeaseLib implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(TeaseLib.class);
    private static final Logger itemLoggerLog = LoggerFactory.getLogger("Items");

    public static final String DefaultDomain = "";
    public static final String DefaultName = "";

    private static final String TranscriptLogFileName = "TeaseLib session transcript.log";

    public final Host host;
    public final Host.AudioSystem audioSystem;
    public final Persistence persistence;
    final UserItems userItems;
    public final TeaseLibLogger transcript;
    public final ItemLogger itemLogger;

    public final Configuration config;
    public final ObjectMap globals = new ObjectMap();
    final StateMaps stateMaps;
    public final Devices devices;
    public final Random random;

    final ApplyRules applyRules = new ApplyRules(ApplyRules.All);

    private final AtomicReference<Thread> timeAdvanceThread = new AtomicReference<>(null);
    private final AtomicLong frozenTime = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong timeOffsetMillis = new AtomicLong(0);
    private final Set<TimeAdvanceListener> timeAdvanceListeners = new HashSet<>();
    private final Set<CheckPointListener> checkPointListeners = new HashSet<>();

    private final Thread mainThread;
    private final Thread saveAllShutDownHook = new Thread(this::saveAll);

    private Runnable quitHandler = null;

    public TeaseLib(Host host, Setup setup) throws IOException {
        Objects.requireNonNull(host);
        Objects.requireNonNull(setup);

        logDateTime();
        logJavaVersion();
        logJavaProperties();

        this.config = new Configuration(setup);
        this.host = host;
        this.audioSystem = new TeaseLibAudioSystem();
        this.persistence = new ConfigFileMapping(config, host.persistence(config));

        this.userItems = new UserItemsImpl(this);
        this.transcript = newTranscriptLogger(host.getLocation(Location.Log));
        this.itemLogger = new ItemLogger(itemLoggerLog::info);

        this.stateMaps = new StateMaps(this);
        this.devices = new Devices(config);

        this.random = new Random();

        this.mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(saveAllShutDownHook);
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

    public static void run(Host host, String script) throws IOException {
        run(host, new TeaseLibConfigSetup(host), script);
    }

    public static void run(Host host, Setup setup, String script) throws IOException {
        try (var teaseLib = new TeaseLib(host, setup)) {
            teaseLib.run(script);
        } catch (ReflectiveOperationException e) {
            throw ExceptionUtil.asRuntimeException(e);
        } finally {
            logger.info("Finished");
        }
    }

    public void setQuitHandler(Runnable quitHandler) {
        this.quitHandler = quitHandler;
        host.setQuitHandler(e -> mainThread.interrupt());
    }

    private void run(String scriptName) throws ReflectiveOperationException {
        host.showInterTitle(Collections.singletonList(""));
        host.show();
        try {
            logger.info("Running script {}", scriptName);
            var contextClassLoader = Thread.currentThread().getContextClassLoader();
            @SuppressWarnings("unchecked")
            Class<RunnableScript> scriptClass = (Class<RunnableScript>) contextClassLoader.loadClass(scriptName);
            RunnableScript script = script(scriptClass);
            script.run();
            globals.get(ScriptRenderer.class).awaitAllCompleted();
        } catch (InterruptedException ignore) {
            // Ignore
        } catch (ScriptInterruptedException e) {
            handleQuit();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            throw t;
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

    private void handleQuit() {
        if (quitHandler != null) {
            try {
                quitHandler.run();
                globals.get(ScriptRenderer.class).awaitAllCompleted();
            } catch (ScriptInterruptedException | InterruptedException ignore) {
                // Ignore
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
                throw t;
            }
        }
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
        try {
            if (Runtime.getRuntime().removeShutdownHook(saveAllShutDownHook)) {
                boolean isInterrupted = Thread.interrupted();
                saveAll();
                cleanup();
                if (isInterrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IllegalStateException e) {
            // saveAll() and cleanup() executed via hook
            if (e.getMessage().contains("Shutdown in progress")) {
                return;
            } else {
                throw e;
            }
        }
    }

    private void saveAll() {
        close(config);
    }

    private void cleanup() {
        close(devices);
        close(globals);
        close(audioSystem);
        if (host instanceof Closeable closeable) {
            close(closeable);
        }
    }

    static void close(Closeable c) {
        try {
            c.close();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    /**
     * Preferred method to wait, since it allows us to test script with automated input and time advance.
     * 
     * @throws InterruptedException
     */
    public void sleep(long duration, TimeUnit unit) throws InterruptedException {
        if (duration > 0) {
            if (isTimeFrozen()) {
                sleepWhileTimeIsFrozen(duration, unit);
            } else {
                sleepInRealTime(duration, unit);
            }
            fireTimeAdvanced();
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    private void sleepWhileTimeIsFrozen(long duration, TimeUnit unit) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (timeAdvanceThread.get() == null || timeAdvanceThread.get() == Thread.currentThread()) {
            advanceTime(duration, unit);
        } else {
            advanceTime(0, unit);
        }
    }

    private void sleepInRealTime(long duration, TimeUnit unit) throws InterruptedException {
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
        TimeUnit unit = TimeUnit.MILLISECONDS;
        long milis = getTime(unit);
        return timeOfDay(milis, unit);
    }

    TimeOfDay timeOfDay(LocalDateTime time, long days) {
        return new TimeOfDayImpl(time, days, itemLogger);
    }

    private TimeOfDay timeOfDay(long time, TimeUnit unit) {
        return new TimeOfDayImpl(localDateTime(time, unit), 0, itemLogger);
    }

    static LocalDateTime localDateTime(long time, TimeUnit unit) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(unit.toMillis(time)), ZoneId.systemDefault());
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
        var start = TimeOfDayImpl.getTime(timeOfDay());
        var end = localDateTime((long) TimeOfDayImpl.hours(dayTime).average() * 60L, TimeUnit.MINUTES);

        long durationMinutes = //
                end.getHour() * 60 + end.getMinute() -
                        start.getHour() * 60 - start.getMinute();

        if (durationMinutes < 0) {
            durationMinutes += 24 * 60 * Math.max(1, daysInTheFuture);
        } else {
            durationMinutes += 24 * 60 * daysInTheFuture;
        }

        return duration(durationMinutes, TimeUnit.MINUTES);
    }

    public abstract class PersistentValue<T> {
        public final QualifiedName name;
        protected T defaultValue;

        PersistentValue(QualifiedName name, T defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        public void clear() {
            persistence.clear(name);
        }

        public boolean available() {
            return persistence.has(name);
        }

        public PersistentValue<T> defaultValue(T newValue) {
            this.defaultValue = newValue;
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

    Map<QualifiedName, PersistentBoolean> persistentBooleanMap = new HashMap<>();

    public PersistentBoolean getBoolean(QualifiedName name) {
        return persistentBooleanMap.computeIfAbsent(name, PersistentBoolean::new);
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent boolean value, start value is false
     */
    public class PersistentBoolean extends PersistentValue<Boolean> {
        public static final boolean DefaultValue = false;

        PersistentBoolean(QualifiedName name) {
            super(name, DefaultValue);
        }

        @Override
        public PersistentBoolean defaultValue(Boolean newValue) {
            super.defaultValue(newValue);
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
            return value();
        }

        public boolean isFalse() {
            return !value();
        }
    }

    Map<QualifiedName, PersistentNumber> persistentNumberMap = new HashMap<>();

    public PersistentNumber getNumber(QualifiedName name) {
        return persistentNumberMap.computeIfAbsent(name, PersistentNumber::new);
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent long value, default value is 0.
     *         <p>
     *         The long value can be used to store dates and time.
     */
    public class PersistentNumber extends PersistentValue<Long> {
        public static final long DefaultValue = 0;

        PersistentNumber(QualifiedName name) {
            super(name, DefaultValue);
        }

        @Override
        public PersistentNumber defaultValue(Long newValue) {
            super.defaultValue(newValue);
            return this;
        }

        public PersistentNumber defaultValue(int newValue) {
            super.defaultValue((long) newValue);
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

        public int intValue() {
            String value = persistence.get(name);
            if (value == null) {
                return defaultValue.intValue();
            } else {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return defaultValue.intValue();
                }
            }
        }

        @Override
        public PersistentNumber set(Long value) {
            persistence.set(name, Long.toString(value));
            return this;
        }

        public PersistentNumber set(int value) {
            persistence.set(name, Integer.toString(value));
            return this;
        }
    }

    Map<QualifiedName, PersistentFloat> persistentFloatMap = new HashMap<>();

    public PersistentFloat getFloat(QualifiedName name) {
        return persistentFloatMap.computeIfAbsent(name, PersistentFloat::new);
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent float value, start value is 0.0
     */
    public class PersistentFloat extends PersistentValue<Double> {
        public static final double DefaultValue = 0.0;

        PersistentFloat(QualifiedName name) {
            super(name, DefaultValue);
        }

        @Override
        public PersistentFloat defaultValue(Double newValue) {
            super.defaultValue(newValue);
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

    Map<QualifiedName, PersistentString> persistentStringMap = new HashMap<>();

    public PersistentString getString(QualifiedName name) {
        return persistentStringMap.computeIfAbsent(name, PersistentString::new);
    }

    /**
     * @author Citizen-Cane
     * 
     *         A persistent String value, start value is the empty string
     */
    public class PersistentString extends PersistentValue<String> {
        public static final String DefaultValue = "";

        PersistentString(QualifiedName name) {
            super(name, DefaultValue);
        }

        @Override
        public PersistentString defaultValue(String newValue) {
            return (PersistentString) super.defaultValue(newValue);
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

    public <T extends Enum<?>> PersistentEnum<T> getEnum(String domain, Class<T> enumClass) {
        return getEnum(QualifiedName.of(
                domain, ReflectionUtils.parent(enumClass), ReflectionUtils.classSimpleName(enumClass)),
                enumClass);
    }

    Map<QualifiedName, PersistentEnum<? extends Enum<?>>> persistentEnumMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends Enum<?>> PersistentEnum<T> getEnum(QualifiedName name, Class<T> enumClass) {
        PersistentEnum<? extends Enum<?>> t = persistentEnumMap.computeIfAbsent(name, k -> {
            PersistentEnum<T> persistentEnum = new PersistentEnum<>(k, enumClass);
            return persistentEnum;
        });
        return (PersistentEnum<T>) t;
    }

    public class PersistentEnum<T extends Enum<?>> extends PersistentValue<T> {

        private PersistentEnum(String domain, Class<T> enumClass) {
            super(QualifiedName.of(domain, parent(enumClass), classSimpleName(enumClass)),
                    enumClass.getEnumConstants()[0]);
        }

        PersistentEnum(QualifiedName name, Class<T> enumClass) {
            super(name, enumClass.getEnumConstants()[0]);
        }

        @Override
        public PersistentEnum<T> defaultValue(T newValue) {
            return (PersistentEnum<T>) super.defaultValue(newValue);
        }

        @Override
        public T value() {
            if (persistence.has(name)) {
                var valueAsString = persistence.get(name);
                @SuppressWarnings("unchecked")
                T value = (T) Enum.valueOf(defaultValue.getClass(), valueAsString);
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
        public PersistentEnum<T> set(T value) {
            persistence.set(name, value.name());
            return this;
        }

    }

    public TextVariables getTextVariables(String domain, Locale locale) {
        var variables = new TextVariables();
        variables.addUserIdentity(this, domain, locale);
        return variables;
    }

    public class PersistentSequence<T extends Enum<T>> {
        public final PersistentString storage;
        public final T[] values;
        private T value;

        @SafeVarargs
        public PersistentSequence(String domain, String namespace, String name, T... values) {
            this.storage = new PersistentString(QualifiedName.of(domain, namespace, name));
            this.values = values;
            String persistedValue = storage.value();
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
            storage.set(value.name());
        }
    }

    /**
     * Return the state of an enumeration member
     * 
     * @param name
     *            The enumeration member to return the state for
     * @return The item state.
     */
    public State state(String domain, Enum<?> qualifiedName) {
        return stateMaps.state(domain, QualifiedString.of(qualifiedName));
    }

    public State state(String domain, String qualifiedName) {
        return stateMaps.state(domain, QualifiedString.of(qualifiedName));
    }

    public State state(String domain, QualifiedString qualifiedName) {
        return stateMaps.state(domain, qualifiedName);
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
            items.addAll(userItems.get(domain, QualifiedString.of(item)));
        }
        return new ItemsImpl(items);
    }

    public Items.Collection items(Item... items) {
        return new ItemsImpl(items);
    }

    public Items.Collection items(Items... items) {
        return new ItemsImpl(items);
    }

    public Items.Collection items(Items.Collection... items) {
        return new ItemsImpl(items);
    }

    public Items.Query items(Items.Query... items) {
        return new ItemsQueryImpl() {
            @Override
            public ItemsImpl inventory() {
                return new ItemsImpl(Arrays.stream(items).map(Items.Query::inventory).flatMap(Items::stream).toList());
            }
        };
    }

    /**
     * Get related items from another domain.
     * 
     * @param domain
     *            The domain to get related items for.
     * @param items
     *            The items for which to retrieve related items from another domain.
     * @return Items similar to those supplied but for the specified domain.
     */
    public Items relatedItems(Enum<?> domain, Items items) {
        return relatedItems(QualifiedString.of(domain), items);
    }

    /**
     * Get related items from another domain.
     * 
     * @param domain
     *            The domain to get related items for.
     * @param items
     *            The items for which to retrieve related items from another domain.
     * @return Items similar to those supplied but for the specified domain.
     */
    public Items relatedItems(QualifiedString domain, Items items) {
        return relatedItems(domain.toString(), items);
    }

    /**
     * Get related items from another domain.
     * 
     * @param domain
     *            The domain to get related items for.
     * @param items
     *            The items for which to retrieve related items from another domain.
     * @return Items similar to those supplied but for the specified domain.
     */

    public Items relatedItems(String domain, Items items) {
        return new ItemsImpl(items.stream().map(AbstractProxy::itemImpl).map(item -> getItem(domain, item)).toList());
    }

    /**
     * Get related items from another domain.
     * 
     * @param domain
     *            The domain to get related items for.
     * @param items
     *            The items for which to retrieve related items from another domain.
     * @return Items similar to those supplied but for the specified domain.
     */
    public Items.Collection relatedItems(String domain, Items.Collection items) {
        return new ItemsImpl(items.stream().map(AbstractProxy::itemImpl).map(item -> getItem(domain, item)).toList());
    }

    /**
     * Get related items from another domain.
     * 
     * @param domain
     *            The domain to get related items for.
     * @param items
     *            The items for which to retrieve related items from another domain.
     * @return Items similar to those supplied but for the specified domain.
     */
    public Items.Set relatedItems(String domain, Items.Set items) {
        return new ItemsImpl(items.stream().map(AbstractProxy::itemImpl).map(item -> getItem(domain, item)).toList());
    }

    /**
     * @return All temporary items
     */
    Items.Collection temporaryItems() {
        Set<Item> temporaryItems = new HashSet<>();
        for (Entry<String, StateMapCache> domains : new ArrayList<>(stateMaps.cache.entrySet())) {
            String domain = domains.getKey();
            ArrayList<Entry<String, StateMap>> namespaces = new ArrayList<>(domains.getValue().entrySet());
            for (Entry<String, StateMap> namespace : namespaces) {
                ArrayList<Entry<QualifiedString, StateImpl>> entries = new ArrayList<>(
                        namespace.getValue().states.entrySet());
                for (Entry<QualifiedString, StateImpl> entry : entries) {
                    StateImpl state = entry.getValue();
                    if (state.name.guid().isEmpty() && state.duration().limit(TimeUnit.SECONDS) == State.TEMPORARY) {
                        List<Item> temporaryPeers = state.peers().stream().filter(QualifiedString::isItem)
                                .map(peer -> getItem(domain, peer)).filter(item -> !item.is(Until.class)).toList();
                        if (!temporaryPeers.isEmpty()) {
                            temporaryItems.addAll(temporaryPeers);
                        }
                    }
                }
            }
        }
        return new ItemsImpl(temporaryItems);
    }

    /**
     * Get the item for any object.
     * 
     * @param namespace
     *            The name space of the item.
     * @param item
     *            The value to get the item for.
     * @return The item that corresponds to the value.
     */
    public <T extends Object> Item item(String domain, T item) {
        if (item instanceof Item) {
            throw new IllegalArgumentException(item.toString());
        } else {
            return ((ItemsImpl) items(domain, item)).get();
        }
    }

    private Item getItem(String domain, ItemImpl item) {
        return getItem(domain, item.kind(), item.name.guid().orElseThrow());
    }

    public Item getItem(String domain, QualifiedString guid) {
        return getItem(domain, guid.kind(), guid.guid().orElseThrow());
    }

    public Item getItem(String domain, QualifiedString item, String guid) {
        var match = findItem(domain, item, guid);
        if (match == Item.NotFound) {
            throw new NoSuchElementException(
                    "Item " + QualifiedName.of(domain, QualifiedName.NONE, item.toString()) + ":" + guid);
        } else {
            return match;
        }
    }

    public Item findItem(String domain, QualifiedString item, String guid) {
        return items(domain, item).stream().filter(ItemImpl.class::isInstance).map(ItemImpl.class::cast)
                .filter(i -> i.name.guid().orElseThrow().equalsIgnoreCase(guid)).map(Item.class::cast).findFirst()
                .orElse(Item.NotFound);
    }

    public Actor getDominant(Gender gender, Locale locale) {
        switch (gender) {
        case Feminine:
            return new Actor("Mistress", "Miss", gender, locale, Actor.Key.DominantFemale, ActorImages.None);
        case Masculine:
            return new Actor("Master", "Sir", gender, locale, Actor.Key.DominantMale, ActorImages.None);
        default:
            throw new IllegalArgumentException(gender.toString());
        }
    }

    public void addUserItems(URL items) {
        userItems.addItems(items);
    }

    public void addUserItems(Collection<Item> items) {
        userItems.addItems(items);
    }

    public <T extends DeviceInteractionImplementation<?, ?>> T deviceInteraction(Class<T> deviceInteraction) {
        return globals.get(DeviceInteractionImplementations.class).get(deviceInteraction);
    }

}
