package teaselib.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

public class Debugger {
    public final TeaseLib teaseLib;

    private final DummyHost dummyHost;
    private final DummyPersistence dummyPersistence;

    public enum Response {
        Choose,
        Ignore
    }

    private Map<String, Response> responses = new LinkedHashMap<String, Response>();

    public Debugger(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;

        this.dummyHost = null;
        this.dummyPersistence = null;
    }

    public Debugger(DummyHost dummyHost, DummyPersistence dummyPersistence) {
        this(new TeaseLib(dummyHost, dummyPersistence), dummyHost, dummyPersistence);
    }

    public Debugger(TeaseLib teaseLib, DummyHost dummyHost, DummyPersistence dummyPersistence) {
        this.teaseLib = teaseLib;

        this.dummyHost = dummyHost;
        this.dummyPersistence = dummyPersistence;

        dummyHost.setResponses(responses);
    }

    public void freezeTime() {
        teaseLib.freezeTime();
    }

    public void resumeTime() {
        teaseLib.resumeTime();
    }

    public void advanceTime(long duration, TimeUnit unit) {
        teaseLib.advanceTime(duration, unit);
    }

    public void addResponse(String match, Response response) {
        responses.put(match, response);
    }
}
