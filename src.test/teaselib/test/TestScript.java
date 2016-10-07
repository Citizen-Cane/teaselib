package teaselib.test;

import java.util.Locale;

import teaselib.Actor;
import teaselib.PersistenceTest;
import teaselib.TeaseScript;
import teaselib.core.ResourceLoader;
import teaselib.core.TeaseLib;
import teaselib.core.texttospeech.Voice;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

public class TestScript extends TeaseScript {
    public final DummyHost host;
    public final DummyPersistence persistence;

    public static final String TestScriptNamespace = TestScript.class
            .getSimpleName() + " " + "Namespace";
    public static final Actor TestScriptActor = new Actor("Test",
            Voice.Gender.Female, Locale.US);

    public static TestScript getOne() {
        return new TestScript(new DummyHost(), new DummyPersistence());
    }

    TestScript(DummyHost dummyHost, DummyPersistence dummyPersistence) {
        super(new TeaseLib(dummyHost, dummyPersistence),
                new ResourceLoader(PersistenceTest.class), TestScriptActor,
                TestScriptNamespace);
        this.host = dummyHost;
        this.persistence = dummyPersistence;
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "namespace=" + namespace + ", storage="
                + persistence.storage.toString();
    }

}
