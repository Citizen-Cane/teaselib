package teaselib.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import teaselib.Actor;
import teaselib.Sexuality.Gender;
import teaselib.TeaseScript;
import teaselib.core.Closeable;
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
import teaselib.util.Select.AbstractStatement;
import teaselib.util.math.Random;

public class TestScript extends TeaseScript implements Closeable {
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

    public TestScript() throws IOException {
        this(new DebugHost());
    }

    public TestScript(PropertyNameMapping propertyNameMapping) throws IOException {
        this(new DebugHost(p -> new PropertyNameMappingPersistence(p, propertyNameMapping)));
    }

    public TestScript(DebugHost debugHost) throws IOException {
        this(debugHost, new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder), new DebugSetup(),
                newActor());
    }

    public TestScript(Setup setup) throws IOException {
        this(new DebugHost(), new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder), setup,
                newActor());
    }

    public TestScript(Actor actor) throws IOException {
        this(new DebugHost(), new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder),
                new DebugSetup(), actor);
    }

    /**
     * @param resourceRoot
     *            The class to use as the root path for all resources. Also selects the package folder.
     *            <p>
     *            Note that the TestScript class is usually in a different source folder than the test. As a result ,
     *            this constructor must be used to access the resources.
     * @throws IOException
     */
    public TestScript(Class<?> resourceRoot) throws IOException {
        this(new DebugHost(), new ResourceLoader(resourceRoot), new DebugSetup(), newActor());
    }

    /**
     * @param resourceRoot
     *            The root path for all resources. Also selects the package folder.
     *            <p>
     *            Note that the TestScript class is usually in a different source folder than the test. As a result ,
     *            this constructor must be used to access the resources.
     * @throws IOException
     */
    public TestScript(String resourceRoot) throws IOException {
        this(new DebugHost(), new ResourceLoader(TestScript.class, resourceRoot), new DebugSetup(), newActor());
    }

    /**
     * @param class1
     * @param string
     */
    public TestScript(Class<?> mainScript, String resourceRoot) throws IOException {
        this(new DebugHost(), new ResourceLoader(mainScript, resourceRoot), new DebugSetup(), newActor());
    }

    TestScript(DebugHost host, ResourceLoader resourceLoader, Setup setup, Actor actor) throws IOException {
        super(new TeaseLib(host, setup), resourceLoader, actor, NAMESPACE);
        this.host = host;
        this.storage = host.persistence.storage;
        this.debugger = new Debugger(teaseLib);
    }

    @Override
    public void close() {
        teaseLib.close();
    }

    public static TeaseLib newTeaseLib() {
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

    public void setAvailable(AbstractStatement... statements) {
        items(statements).inventory().stream().forEach(item -> item.setAvailable(true));
    }

    public void setAvailable(Enum<?>... items) {
        items(items).inventory().stream().forEach(item -> item.setAvailable(true));
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

    public Random randomNumbers(Integer... numbers) {
        Iterator<Integer> sequence = Arrays.asList(numbers).iterator();
        return new Random(new RandomGenerator() {
            @Override
            public long nextLong() {
                return sequence.next();
            }
        });
    }
}
