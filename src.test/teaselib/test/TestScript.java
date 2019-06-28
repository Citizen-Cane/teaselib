package teaselib.test;

import java.io.IOException;
import java.util.Locale;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.TeaseScript;
import teaselib.core.Debugger;
import teaselib.core.Persistence;
import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.ScriptEvents;
import teaselib.core.TeaseLib;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.configuration.Setup;
import teaselib.core.debug.DebugHost;
import teaselib.core.debug.DebugPersistence;
import teaselib.core.debug.DebugStorage;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.PropertyNameMapping;
import teaselib.core.util.PropertyNameMappingPersistence;

public class TestScript extends TeaseScript {
    public final DebugHost host;
    public final Persistence persistence;
    public final DebugStorage storage;
    public final Debugger debugger;

    public static final String NAMESPACE = TestScript.class.getSimpleName() + " " + "Namespace";

    public static final Actor newActor() {
        return newActor(Gender.Feminine);
    }

    public static Actor newActor(Gender gender) {
        return newActor(gender, Locale.US);
    }

    public static Actor newActor(Gender gender, Locale locale) {
        return new Actor("Test", gender, locale);
    }

    public static TestScript getOne() {
        DebugStorage storage = new DebugStorage();
        try {
            return new TestScript(new DebugHost(), newDebugPersistence(storage), storage);
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(Actor actor) {
        DebugStorage storage = new DebugStorage();
        try {
            return new TestScript(new DebugHost(), newDebugPersistence(storage), storage, new DebugSetup(), actor);
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(Setup setup) {
        DebugStorage storage = new DebugStorage();
        try {
            return new TestScript(new DebugHost(), newDebugPersistence(storage), storage, setup, newActor());
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(Class<?> resourceRoot) {
        DebugStorage storage = new DebugStorage();
        try {
            return new TestScript(new DebugHost(), newDebugPersistence(storage), storage, resourceRoot);
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(String resourceRoot) {
        DebugStorage storage = new DebugStorage();
        try {
            return new TestScript(new DebugHost(), newDebugPersistence(storage), storage, resourceRoot, newActor());
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(PropertyNameMapping propertyNameMapping) {
        DebugStorage storage = new DebugStorage();
        try {
            return new TestScript(new DebugHost(), newDebugPersistence(propertyNameMapping, storage), storage);
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public TestScript(DebugHost debugHost, Persistence debugPersistence, DebugStorage storage) throws IOException {
        this(debugHost, debugPersistence, storage,
                new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder), new DebugSetup(),
                newActor());
    }

    public TestScript(DebugHost debugHost, Persistence debugPersistence, DebugStorage storage, Setup setup)
            throws IOException {
        this(debugHost, debugPersistence, storage,
                new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder), setup, newActor());
    }

    TestScript(DebugHost debugHost, Persistence debugPersistence, DebugStorage storage, Setup setup, Actor actor)
            throws IOException {
        this(debugHost, debugPersistence, storage,
                new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder), setup, actor);
    }

    TestScript(DebugHost debugHost, Persistence debugPersistence, DebugStorage storage, Class<?> resourceRoot)
            throws IOException {
        this(debugHost, debugPersistence, storage, new ResourceLoader(resourceRoot), new DebugSetup(), newActor());
    }

    public TestScript(DebugHost debugHost, Persistence debugPersistence, DebugStorage storage, String resourceRoot,
            Actor actor) throws IOException {
        this(debugHost, debugPersistence, storage, new ResourceLoader(TestScript.class, resourceRoot), new DebugSetup(),
                actor);
    }

    @SuppressWarnings("resource")
    TestScript(DebugHost host, Persistence persistence, DebugStorage storage, ResourceLoader resourceLoader,
            Setup setup, Actor actor) throws IOException {
        super(new TeaseLib(host, persistence, setup), resourceLoader, actor, NAMESPACE);
        this.host = host;
        this.persistence = persistence;
        this.storage = storage;
        this.debugger = new Debugger(teaseLib);
    }

    public static TeaseLib teaseLib() {
        DebugStorage storage = new DebugStorage();
        try {
            return new TeaseLib(new DebugHost(), newDebugPersistence(storage), new DebugSetup());
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static void run(Class<? extends Script> script) {
        DebugStorage storage = new DebugStorage();
        try {
            TeaseLib.run(new DebugHost(), newDebugPersistence(storage), new DebugSetup(), script.getName());
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    protected static Persistence newDebugPersistence(DebugStorage storage) {
        return newDebugPersistence(PropertyNameMapping.NONE, storage);
    }

    private static Persistence newDebugPersistence(PropertyNameMapping propertyNameMapping, DebugStorage storage) {
        return new PropertyNameMappingPersistence(new DebugPersistence(storage), propertyNameMapping);
    }

    public void addTestUserItems() {
        addCustomUserItems("useritems.xml");
    }

    public void addTestUserItems2() {
        addCustomUserItems("useritems2.xml");
    }

    public void setAvailable(Enum<?>... items) {
        items(items).stream().forEach(item -> item.setAvailable(true));
    }

    private void addCustomUserItems(String resourcePath) {
        teaseLib.addUserItems(getClass().getResource(resourcePath));
    }

    public ScriptEvents events() {
        return events;
    }

    @Override
    public String toString() {
        return "namespace=" + namespace + ", storage=" + storage;
    }
}
