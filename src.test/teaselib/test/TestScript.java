package teaselib.test;

import java.io.IOException;
import java.util.Locale;

import teaselib.Actor;
import teaselib.TeaseScript;
import teaselib.core.Configuration.Setup;
import teaselib.core.Debugger;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.Voice;
import teaselib.core.util.PropertyNameMapping;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

public class TestScript extends TeaseScript {
    public final DummyHost host;
    public final DummyPersistence persistence;
    public final Debugger debugger;

    public static final String TestScriptNamespace = TestScript.class.getSimpleName() + " " + "Namespace";
    public static final Actor TestScriptActor = new Actor("Test", Voice.Gender.Female, Locale.US);

    public static TestScript getOne() {
        try {
            return new TestScript(new DummyHost(), new DummyPersistence());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestScript getOne(Setup setup) {
        try {
            return new TestScript(new DummyHost(), new DummyPersistence(), setup);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestScript getOne(PropertyNameMapping propertyNameMapping) {
        try {
            return new TestScript(new DummyHost(), new DummyPersistence(propertyNameMapping));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestScript getOne(Class<?> resourceRoot) {
        try {
            return new TestScript(new DummyHost(), new DummyPersistence(), resourceRoot);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    TestScript(DummyHost dummyHost, DummyPersistence dummyPersistence) throws IOException {
        this(dummyHost, dummyPersistence, new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder),
                new DebugSetup());
    }

    TestScript(DummyHost dummyHost, DummyPersistence dummyPersistence, Setup setup) throws IOException {
        this(dummyHost, dummyPersistence, new ResourceLoader(TestScript.class, ResourceLoader.ResourcesInProjectFolder),
                setup);
    }

    TestScript(DummyHost dummyHost, DummyPersistence dummyPersistence, Class<?> resourceRoot) throws IOException {
        this(dummyHost, dummyPersistence, new ResourceLoader(resourceRoot), new DebugSetup());
    }

    TestScript(DummyHost dummyHost, DummyPersistence dummyPersistence, ResourceLoader resourceLoader, Setup setup)
            throws IOException {
        super(new TeaseLib(dummyHost, dummyPersistence, setup), resourceLoader, TestScriptActor, TestScriptNamespace);
        this.host = dummyHost;
        this.persistence = dummyPersistence;
        this.debugger = new Debugger(teaseLib);
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "namespace=" + namespace + ", storage=" + persistence.storage.toString();
    }

}
