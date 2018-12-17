package teaselib.test;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Function;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.TeaseScript;
import teaselib.core.Debugger;
import teaselib.core.Persistence;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.configuration.Setup;
import teaselib.core.debug.DebugHost;
import teaselib.core.debug.DebugPersistence;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.PropertyNameMapping;

public class TestScript extends TeaseScript {
    public final DebugHost host;
    public final DebugPersistence persistence;
    public final Debugger debugger;

    public static final String TestScriptNamespace = TestScript.class.getSimpleName() + " " + "Namespace";

    public static final Actor newActor() {
        return newActor(Gender.Feminine);
    }

    public static Actor newActor(Gender gender) {
        return new Actor("Test", gender, Locale.US);
    }

    public static TestScript getOne() {
        try {
            return new TestScript();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestScript getOne(Actor actor) {
        try {
            return new TestScript(new DebugHost(), new DebugPersistence(), new DebugSetup(), actor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestScript getOne(Setup setup) {
        try {
            return new TestScript(new DebugHost(), new DebugPersistence(), setup, newActor());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestScript getOne(Function<Persistence, PropertyNameMapping> propertyNameMapping) {
        try {
            return new TestScript(new DebugHost(), new DebugPersistence(propertyNameMapping));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestScript getOne(Class<?> resourceRoot) {
        try {
            return new TestScript(new DebugHost(), new DebugPersistence(), resourceRoot);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestScript getOne(String resourceRoot) {
        try {
            return new TestScript(new DebugHost(), new DebugPersistence(), resourceRoot, newActor());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected TestScript() throws IOException {
        this(new DebugHost(), new DebugPersistence());
    }

    protected TestScript(Setup setup) throws IOException {
        this(new DebugHost(), new DebugPersistence(), setup, newActor());
    }

    public TestScript(DebugHost dummyHost, DebugPersistence dummyPersistence) throws IOException {
        this(dummyHost, dummyPersistence, new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder),
                new DebugSetup(), newActor());
    }

    TestScript(DebugHost dummyHost, DebugPersistence dummyPersistence, Setup setup, Actor actor) throws IOException {
        this(dummyHost, dummyPersistence, new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder),
                setup, actor);
    }

    TestScript(DebugHost dummyHost, DebugPersistence dummyPersistence, Class<?> resourceRoot) throws IOException {
        this(dummyHost, dummyPersistence, new ResourceLoader(resourceRoot), new DebugSetup(), newActor());
    }

    public TestScript(DebugHost debugHost, DebugPersistence debugPersistence, String resourceRoot, Actor actor)
            throws IOException {
        this(debugHost, debugPersistence, new ResourceLoader(TestScript.class, resourceRoot), new DebugSetup(), actor);
    }

    TestScript(DebugHost host, DebugPersistence persistence, ResourceLoader resourceLoader, Setup setup, Actor actor)
            throws IOException {
        super(new TeaseLib(host, persistence, setup), resourceLoader, actor, TestScriptNamespace);
        this.host = host;
        this.persistence = persistence;
        this.debugger = new Debugger(teaseLib);
    }

    public static TeaseLib setup() {
        try {
            return new TeaseLib(new DebugHost(), new DebugPersistence(), new DebugSetup());
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
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

    @Override
    public String toString() {
        return "namespace=" + namespace + ", storage=" + persistence.storage.toString();
    }
}
