package teaselib.test;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.TeaseScript;
import teaselib.core.Debugger;
import teaselib.core.ResourceLoader;
import teaselib.core.Script;
import teaselib.core.ScriptEvents;
import teaselib.core.TeaseLib;
import teaselib.core.configuration.DebugSetup;
import teaselib.core.configuration.Setup;
import teaselib.core.debug.DebugHost;
import teaselib.core.debug.DebugStorage;
import teaselib.core.util.ExceptionUtil;
import teaselib.core.util.PropertyNameMapping;
import teaselib.core.util.PropertyNameMappingPersistence;
import teaselib.core.util.QualifiedName;

public class TestScript extends TeaseScript {
    public final DebugHost host;
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
        try {
            return new TestScript(new DebugHost());
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(Actor actor) {
        try {
            return new TestScript(new DebugHost(), new DebugSetup(), actor);
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(Setup setup) {
        try {
            return new TestScript(new DebugHost(), setup, newActor());
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(Class<?> resourceRoot) {
        try {
            return new TestScript(new DebugHost(), resourceRoot);
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(String resourceRoot) {
        try {
            return new TestScript(new DebugHost(), resourceRoot, newActor());
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static TestScript getOne(PropertyNameMapping propertyNameMapping) {
        try {
            DebugHost debugHost = new DebugHost(p -> new PropertyNameMappingPersistence(p, propertyNameMapping));
            return new TestScript(debugHost);
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public TestScript(DebugHost debugHost) throws IOException {
        this(debugHost, new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder), new DebugSetup(),
                newActor());
    }

    public TestScript(DebugHost debugHost, Setup setup) throws IOException {
        this(debugHost, new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder), setup,
                newActor());
    }

    TestScript(DebugHost debugHost, Setup setup, Actor actor) throws IOException {
        this(debugHost, new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder), setup, actor);
    }

    TestScript(DebugHost debugHost, Class<?> resourceRoot) throws IOException {
        this(debugHost, new ResourceLoader(resourceRoot), new DebugSetup(), newActor());
    }

    public TestScript(DebugHost debugHost, String resourceRoot, Actor actor) throws IOException {
        this(debugHost, new ResourceLoader(TestScript.class, resourceRoot), new DebugSetup(), actor);
    }

    TestScript(DebugHost host, ResourceLoader resourceLoader, Setup setup, Actor actor) throws IOException {
        super(new TeaseLib(host, setup), resourceLoader, actor, NAMESPACE);
        this.host = host;
        this.storage = host.persistence.storage;
        this.debugger = new Debugger(teaseLib);
    }

    public static TeaseLib teaseLib() {
        try {
            return new TeaseLib(new DebugHost(), new DebugSetup());
        } catch (IOException e) {
            throw ExceptionUtil.asRuntimeException(e);
        }
    }

    public static void run(Class<? extends Script> script) {
        try (DebugHost host = new DebugHost()) {
            TeaseLib.run(host, new DebugSetup(), script.getName());
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

    public ScriptEvents events() {
        return scriptRenderer.events;
    }

    public void triggerAutoRemove() {
        handleAutoRemove();
    }

    public int storageSize() {
        List<QualifiedName> keys = storage.keySet().stream() //
                .filter(key -> !key.domain.startsWith("LastUsed")) //
                .filter(key -> !key.namespace.startsWith("LastUsed")) //
                // PersistedDomain values are present since they reference Domain.LastUsed
                .filter(key -> !key.namespace.startsWith("PersistedDomains")) //
                .collect(Collectors.toList());
        return keys.size();
    }

    @Override
    public String toString() {
        return "namespace=" + namespace + ", storage=" + storage;
    }
}
