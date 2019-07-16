package teaselib.core;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import teaselib.core.ui.InputMethods;
import teaselib.util.Daytime;

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
        Ignore,
        Invoke
    }

    private final DebugInputMethod debugInputMethod;

    public Debugger(Script script) {
        this(script.teaseLib);
    }

    public Debugger(Script script, DebugInputMethod debugInputMethod) {
        this(script.teaseLib, debugInputMethod);
    }

    public Debugger(TeaseLib teaseLib) {
        this(teaseLib, () -> {
        });
    }

    public Debugger(TeaseLib teaseLib, Runnable debugInputMethodHandler) {
        this(teaseLib, new ResponseDebugInputMethod(debugInputMethodHandler));
    }

    public Debugger(TeaseLib teaseLib, DebugInputMethod debugInputMethod) {
        this.teaseLib = teaseLib;
        this.debugInputMethod = debugInputMethod;
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

    public void advanceTimeAllThreads() {
        teaseLib.advanceTimeAllThreads();
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

    public void addResponse(String match, Callable<Response> response) {
        // TODO Resolve cast, for instance by input method priority (first coverage, then defined response)
        // -> use multiple debug methods
        ((ResponseDebugInputMethod) debugInputMethod).getResponses().add(new ResponseAction(match, response));
    }

    public void addResponse(ResponseAction responseAction) {
        ((ResponseDebugInputMethod) debugInputMethod).getResponses().add(responseAction);
    }

    public void clearStateMaps() {
        teaseLib.stateMaps.clear();
        ((UserItemsImpl) teaseLib.userItems).clearCachedItems();
    }

    public void addResponses(Collection<ResponseAction> responses) {
        for (ResponseAction responseAction : responses) {
            addResponse(responseAction);
        }
    }

    public void replyScriptFunction(String string) {
        ((ResponseDebugInputMethod) debugInputMethod).replyScriptFunction(string);
    }

    public void setTime(Daytime dayTime) {
        Date now = new Date(teaseLib.getTime(TimeUnit.MILLISECONDS));
        // TODO Resolve deprecation
        Date adjusted = new Date(now.getYear(), now.getMonth(), now.getDate(), 0, 0, 0);
        teaseLib.advanceTime(adjusted.getTime() - now.getTime(), TimeUnit.MILLISECONDS);

        long hours = TimeOfDayImpl.hours(dayTime).average();
        teaseLib.advanceTime(hours, TimeUnit.HOURS);
    }

}
