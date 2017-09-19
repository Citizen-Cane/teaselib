package teaselib.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    private final Map<String, Response> responses = new LinkedHashMap<String, Response>();
    private final DebugInputMethod debugInputMethod;

    public Debugger(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
        this.debugInputMethod = new DebugInputMethod();
        attach();
    }

    public void attach() {
        freezeTime();
        teaseLib.hostInputMethods.add(debugInputMethod);
    }

    public void detach() {
        resumeTime();
        teaseLib.hostInputMethods.remove(debugInputMethod);
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
        debugInputMethod.getResponses().add(match,
                response == Response.Choose ? DebugResponses.IMMEDIATELY : DebugResponses.NEVER);
    }

    public void addResponse(ResponseAction responseAction) {
        addResponse(responseAction.match, responseAction.response);
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
