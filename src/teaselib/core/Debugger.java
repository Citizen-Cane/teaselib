package teaselib.core;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import teaselib.core.Configuration.Setup;
import teaselib.hosts.DummyHost;
import teaselib.hosts.DummyPersistence;

public class Debugger {
    public final TeaseLib teaseLib;

    public static class ResponseAction {
        final String match;
        final Response response;

        public ResponseAction(String match, Response response) {
            super();
            this.match = match;
            this.response = response;
        }
    }

    public enum Response {
        Choose,
        Ignore
    }

    private Map<String, Response> responses = new LinkedHashMap<String, Response>();

    public Debugger(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
    }

    public Debugger(DummyHost dummyHost, DummyPersistence dummyPersistence) throws IOException {
        this(new TeaseLib(dummyHost, dummyPersistence), dummyHost);
    }

    public Debugger(DummyHost dummyHost, DummyPersistence dummyPersistence, Setup setup) throws IOException {
        this(new TeaseLib(dummyHost, dummyPersistence, setup), dummyHost);
    }

    public Debugger(TeaseLib teaseLib, DummyHost dummyHost) {
        this.teaseLib = teaseLib;

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

    public void addResponse(ResponseAction responseAction) {
        responses.put(responseAction.match, responseAction.response);
    }

    public void clearStateMaps() {
        teaseLib.stateMaps.clear();
    }

    public void addResponses(Collection<ResponseAction> responses) {
        for (ResponseAction responseAction : responses) {
            addResponse(responseAction);
        }
    }
}
