package teaselib.core;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import teaselib.core.ui.InputMethods;

public class Debugger {
    public final TeaseLib teaseLib;

    public static class ResponseAction {
        public final String match;
        private final Callable<Response> response;

        public ResponseAction(String match) {
            this(match, Response.Choose);
        }

        public ResponseAction(String match, Response response) {
            super();
            this.match = match;
            this.response = () -> response;
        }

        public ResponseAction(String match, Callable<Response> response) {
            super();
            this.match = match;
            this.response = response;
        }

        public Callable<Response> getResponse() {
            return response;
        }

        @Override
        public String toString() {
            String s = match + "->";
            try {
                return s + response.call();
            } catch (Exception e) {
                return s + e.getMessage();
            }
        }
    }

    public enum Response {
        Choose,
        Ignore
    }

    private final DebugInputMethod debugInputMethod;

    public Debugger(TeaseLib teaseLib) {
        this.teaseLib = teaseLib;
        this.debugInputMethod = new DebugInputMethod();
        attach();
    }

    public void attach() {
        freezeTime();
        teaseLib.globals.get(InputMethods.class).add(debugInputMethod);
        debugInputMethod.attach(teaseLib);
    }

    public void detach() {
        debugInputMethod.detach(teaseLib);
        teaseLib.globals.get(InputMethods.class).remove(debugInputMethod);
        resumeTime();
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
        addResponse(new ResponseAction(match, response));
    }

    public void addResponse(ResponseAction responseAction) {
        debugInputMethod.getResponses().add(responseAction);
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
